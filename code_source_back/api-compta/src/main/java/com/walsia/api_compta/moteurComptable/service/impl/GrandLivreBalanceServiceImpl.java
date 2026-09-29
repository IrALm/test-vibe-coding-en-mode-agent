package com.walsia.api_compta.moteurComptable.service.impl;

import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.moteurComptable.dto.readDto.BalanceLigneReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.BalanceReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.GrandLivreLigneReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.GrandLivreReadDto;
import com.walsia.api_compta.moteurComptable.entity.ecriture.Ecriture;
import com.walsia.api_compta.moteurComptable.entity.ecriture.LigneEcriture;
import com.walsia.api_compta.moteurComptable.entity.exercice.ExerciceComptable;
import com.walsia.api_compta.moteurComptable.repository.ExerciceComptableRepository;
import com.walsia.api_compta.moteurComptable.repository.LigneEcritureRepository;
import com.walsia.api_compta.moteurComptable.service.interfaces.GrandLivreBalanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GrandLivreBalanceServiceImpl implements GrandLivreBalanceService {

    private final UtilisateurRepository utilisateurRepository;
    private final ExerciceComptableRepository exerciceComptableRepository;
    private final CompteComptableRepository compteComptableRepository;
    private final LigneEcritureRepository ligneEcritureRepository;

    public GrandLivreBalanceServiceImpl(UtilisateurRepository utilisateurRepository,
                                         ExerciceComptableRepository exerciceComptableRepository,
                                         CompteComptableRepository compteComptableRepository,
                                         LigneEcritureRepository ligneEcritureRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.exerciceComptableRepository = exerciceComptableRepository;
        this.compteComptableRepository = compteComptableRepository;
        this.ligneEcritureRepository = ligneEcritureRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GrandLivreReadDto obtenirGrandLivre(String keycloakIdAppelant, String exerciceId, String compteId) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        ExerciceComptable exercice = exerciceDeLEntite(entite, exerciceId);
        CompteComptable compte = compteVisible(compteId, entite.getId());

        // Solde d'ouverture (à-nouveaux) : cumul depuis l'origine pour un compte de bilan (classes 1-5,
        // le solde se reporte d'exercice en exercice) - toujours 0 pour un compte de gestion (6/7/8, remis
        // à zéro chaque exercice, sans quoi le résultat calculé sur l'exercice courant serait faussé).
        BigDecimal soldeOuverture = estCompteDeBilan(compte)
                ? ligneEcritureRepository.calculerSoldeAvant(compte.getId(), exercice.getDateDebut())
                : BigDecimal.ZERO;

        List<LigneEcriture> mouvements = ligneEcritureRepository.findGrandLivre(compte.getId(), exercice.getId());
        List<GrandLivreLigneReadDto> lignes = new ArrayList<>();
        BigDecimal soldeProgressif = soldeOuverture;
        for (LigneEcriture ligne : mouvements) {
            BigDecimal debit = ligne.getSens() == SensCompte.DEBIT ? ligne.getMontant() : BigDecimal.ZERO;
            BigDecimal credit = ligne.getSens() == SensCompte.CREDIT ? ligne.getMontant() : BigDecimal.ZERO;
            soldeProgressif = soldeProgressif.add(credit).subtract(debit);
            Ecriture ecriture = ligne.getEcriture();
            String libelleLigne = ligne.getLibelle() != null ? ligne.getLibelle() : ecriture.getLibelle();
            lignes.add(new GrandLivreLigneReadDto(ecriture.getDate(), ecriture.getId(), ecriture.getNumero(),
                    ecriture.getReference(), libelleLigne, debit, credit, soldeProgressif));
        }

        return new GrandLivreReadDto(compte.getId(), compte.getNumero(), compte.getLibelle(),
                soldeOuverture, lignes, soldeProgressif);
    }

    /** Compte-tampon pour fusionner mouvements de l'exercice et solde d'ouverture par compte,
     *  avant de calculer le solde de clôture (cf. obtenirBalance). */
    private record LigneBalanceBrute(String numero, String libelle, BigDecimal debit, BigDecimal credit,
                                      BigDecimal soldeOuverture) {
        private LigneBalanceBrute avecSoldeOuverture(BigDecimal soldeOuverture) {
            return new LigneBalanceBrute(numero, libelle, debit, credit, soldeOuverture);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceReadDto obtenirBalance(String keycloakIdAppelant, String exerciceId) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        ExerciceComptable exercice = exerciceDeLEntite(entite, exerciceId);

        // Fusion mouvements de l'exercice + solde d'ouverture (comptes de bilan uniquement, cf.
        // calculerSoldesOuvertureBilan) - même principe que le grand livre, pour que le solde d'un
        // compte donné soit identique sur les deux écrans (§1.6 critère 5 du cahier des charges).
        Map<String, LigneBalanceBrute> parCompte = new LinkedHashMap<>();
        for (Object[] m : ligneEcritureRepository.findBalance(exercice.getId(), entite.getId())) {
            parCompte.put((String) m[0], new LigneBalanceBrute(
                    (String) m[1], (String) m[2], (BigDecimal) m[3], (BigDecimal) m[4], BigDecimal.ZERO));
        }
        for (Object[] o : ligneEcritureRepository.calculerSoldesOuvertureBilan(entite.getId(), exercice.getDateDebut())) {
            String compteId = (String) o[0];
            BigDecimal soldeOuverture = (BigDecimal) o[3];
            LigneBalanceBrute existante = parCompte.get(compteId);
            parCompte.put(compteId, existante != null
                    ? existante.avecSoldeOuverture(soldeOuverture)
                    : new LigneBalanceBrute((String) o[1], (String) o[2], BigDecimal.ZERO, BigDecimal.ZERO, soldeOuverture));
        }

        List<BalanceLigneReadDto> lignes = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Map.Entry<String, LigneBalanceBrute> entree : parCompte.entrySet()) {
            LigneBalanceBrute l = entree.getValue();
            BigDecimal solde = l.soldeOuverture().add(l.credit()).subtract(l.debit());
            BigDecimal soldeDebiteur = solde.signum() < 0 ? solde.negate() : BigDecimal.ZERO;
            BigDecimal soldeCrediteur = solde.signum() > 0 ? solde : BigDecimal.ZERO;
            lignes.add(new BalanceLigneReadDto(entree.getKey(), l.numero(), l.libelle(), l.debit(), l.credit(), soldeDebiteur, soldeCrediteur));
            totalDebit = totalDebit.add(l.debit());
            totalCredit = totalCredit.add(l.credit());
        }
        lignes.sort(Comparator.comparing(BalanceLigneReadDto::compteNumero));

        return new BalanceReadDto(lignes, totalDebit, totalCredit);
    }

    private boolean estCompteDeBilan(CompteComptable compte) {
        int classe = compte.getClasseCompteComptable().getNumero();
        return classe >= 1 && classe <= 5;
    }

    private CompteComptable compteVisible(String compteId, String entiteId) {
        CompteComptable compte = compteComptableRepository.findById(compteId)
                .orElseThrow(() -> new RessourceIntrouvableException("Compte comptable introuvable"));
        boolean visible = compte.getEntite() == null || compte.getEntite().getId().equals(entiteId);
        if (!visible) {
            throw new RessourceIntrouvableException("Compte comptable introuvable");
        }
        return compte;
    }

    private Entite entiteAppelant(String keycloakIdAppelant) {
        return utilisateurRepository.findByKeycloakId(keycloakIdAppelant)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"))
                .getEntite();
    }

    private ExerciceComptable exerciceDeLEntite(Entite entite, String exerciceId) {
        ExerciceComptable exercice = exerciceComptableRepository.findById(exerciceId)
                .orElseThrow(() -> new RessourceIntrouvableException("Exercice introuvable"));
        if (!exercice.getEntite().getId().equals(entite.getId())) {
            throw new RessourceIntrouvableException("Exercice introuvable");
        }
        return exercice;
    }
}
