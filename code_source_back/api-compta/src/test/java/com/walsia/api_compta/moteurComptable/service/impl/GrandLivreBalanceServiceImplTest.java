package com.walsia.api_compta.moteurComptable.service.impl;

import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.ClasseCompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.moteurComptable.dto.readDto.BalanceReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.GrandLivreReadDto;
import com.walsia.api_compta.moteurComptable.entity.ecriture.Ecriture;
import com.walsia.api_compta.moteurComptable.entity.ecriture.LigneEcriture;
import com.walsia.api_compta.moteurComptable.entity.exercice.ExerciceComptable;
import com.walsia.api_compta.moteurComptable.entity.exercice.StatutExercice;
import com.walsia.api_compta.moteurComptable.repository.ExerciceComptableRepository;
import com.walsia.api_compta.moteurComptable.repository.LigneEcritureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrandLivreBalanceServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ExerciceComptableRepository exerciceComptableRepository;
    @Mock
    private CompteComptableRepository compteComptableRepository;
    @Mock
    private LigneEcritureRepository ligneEcritureRepository;

    private GrandLivreBalanceServiceImpl service;
    private Entite entite;
    private ExerciceComptable exercice;

    @BeforeEach
    void setUp() {
        service = new GrandLivreBalanceServiceImpl(
                utilisateurRepository, exerciceComptableRepository, compteComptableRepository, ligneEcritureRepository);

        entite = Entite.builder().id("entite-1").build();
        exercice = ExerciceComptable.builder().id("ex-1").entite(entite).statut(StatutExercice.OUVERT)
                .dateDebut(LocalDate.of(2026, 1, 1)).dateFin(LocalDate.of(2026, 12, 31)).build();
        Utilisateur appelant = Utilisateur.builder().id("user-1").keycloakId("kc-user").entite(entite).build();

        lenient().when(utilisateurRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(appelant));
        lenient().when(exerciceComptableRepository.findById("ex-1")).thenReturn(Optional.of(exercice));
    }

    private CompteComptable compte(String id, String numero, int classeNumero) {
        ClasseCompteComptable classe = ClasseCompteComptable.builder().id("classe-" + classeNumero).numero(classeNumero).build();
        return CompteComptable.builder().id(id).numero(numero).libelle("Compte " + numero).classeCompteComptable(classe).build();
    }

    @Test
    void obtenirGrandLivre_compteDeBilan_utiliseSoldeAvantCommeOuverture() {
        CompteComptable compteClients = compte("compte-1", "411", 4);
        when(compteComptableRepository.findById("compte-1")).thenReturn(Optional.of(compteClients));
        when(ligneEcritureRepository.calculerSoldeAvant("compte-1", exercice.getDateDebut()))
                .thenReturn(new BigDecimal("500.00"));
        when(ligneEcritureRepository.findGrandLivre("compte-1", "ex-1")).thenReturn(List.of());

        GrandLivreReadDto resultat = service.obtenirGrandLivre("kc-user", "ex-1", "compte-1");

        assertThat(resultat.soldeOuverture()).isEqualByComparingTo("500.00");
        assertThat(resultat.soldeCloture()).isEqualByComparingTo("500.00");
    }

    @Test
    void obtenirGrandLivre_compteDeGestion_soldeOuvertureToujoursZero() {
        CompteComptable compteVentes = compte("compte-2", "701", 7);
        when(compteComptableRepository.findById("compte-2")).thenReturn(Optional.of(compteVentes));
        when(ligneEcritureRepository.findGrandLivre("compte-2", "ex-1")).thenReturn(List.of());

        GrandLivreReadDto resultat = service.obtenirGrandLivre("kc-user", "ex-1", "compte-2");

        assertThat(resultat.soldeOuverture()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(ligneEcritureRepository, never()).calculerSoldeAvant(any(), any());
    }

    @Test
    void obtenirGrandLivre_calculeLeSoldeProgressif() {
        CompteComptable compteClients = compte("compte-1", "411", 4);
        when(compteComptableRepository.findById("compte-1")).thenReturn(Optional.of(compteClients));
        when(ligneEcritureRepository.calculerSoldeAvant("compte-1", exercice.getDateDebut())).thenReturn(BigDecimal.ZERO);

        Ecriture ecriture1 = Ecriture.builder().id("ecr-1").date(LocalDate.of(2026, 3, 1)).numero("000001").build();
        LigneEcriture ligne1 = LigneEcriture.builder().ecriture(ecriture1).compte(compteClients)
                .sens(SensCompte.DEBIT).montant(new BigDecimal("100.00")).build();
        Ecriture ecriture2 = Ecriture.builder().id("ecr-2").date(LocalDate.of(2026, 3, 5)).numero("000002").build();
        LigneEcriture ligne2 = LigneEcriture.builder().ecriture(ecriture2).compte(compteClients)
                .sens(SensCompte.CREDIT).montant(new BigDecimal("30.00")).build();
        when(ligneEcritureRepository.findGrandLivre("compte-1", "ex-1")).thenReturn(List.of(ligne1, ligne2));

        GrandLivreReadDto resultat = service.obtenirGrandLivre("kc-user", "ex-1", "compte-1");

        assertThat(resultat.lignes().get(0).soldeProgressif()).isEqualByComparingTo("-100.00");
        assertThat(resultat.lignes().get(1).soldeProgressif()).isEqualByComparingTo("-70.00");
        assertThat(resultat.soldeCloture()).isEqualByComparingTo("-70.00");
    }

    @Test
    void obtenirGrandLivre_compteDuneAutreEntiteNonPartage_leveRessourceIntrouvable() {
        Entite autreEntite = Entite.builder().id("entite-2").build();
        CompteComptable compteAutreEntite = CompteComptable.builder().id("compte-1").entite(autreEntite).build();
        when(compteComptableRepository.findById("compte-1")).thenReturn(Optional.of(compteAutreEntite));

        assertThatThrownBy(() -> service.obtenirGrandLivre("kc-user", "ex-1", "compte-1"))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void obtenirGrandLivre_exerciceDuneAutreEntite_leveRessourceIntrouvable() {
        Entite autreEntite = Entite.builder().id("entite-2").build();
        ExerciceComptable exerciceAutreEntite = ExerciceComptable.builder().id("ex-2").entite(autreEntite).build();
        when(exerciceComptableRepository.findById("ex-2")).thenReturn(Optional.of(exerciceAutreEntite));

        assertThatThrownBy(() -> service.obtenirGrandLivre("kc-user", "ex-2", "compte-1"))
                .isInstanceOf(RessourceIntrouvableException.class);
    }

    @Test
    void obtenirBalance_totalDebitEgaleTotalCreditEtCalculeLesSoldes() {
        List<Object[]> lignesBrutes = List.of(
                new Object[]{"compte-1", "411", "Clients", new BigDecimal("100.00"), BigDecimal.ZERO},
                new Object[]{"compte-2", "701", "Ventes", BigDecimal.ZERO, new BigDecimal("100.00")});
        when(ligneEcritureRepository.findBalance("ex-1", "entite-1")).thenReturn(lignesBrutes);
        when(ligneEcritureRepository.calculerSoldesOuvertureBilan("entite-1", exercice.getDateDebut()))
                .thenReturn(List.of());

        BalanceReadDto resultat = service.obtenirBalance("kc-user", "ex-1");

        assertThat(resultat.totalDebit()).isEqualByComparingTo(resultat.totalCredit());
        assertThat(resultat.lignes().get(0).soldeDebiteur()).isEqualByComparingTo("100.00");
        assertThat(resultat.lignes().get(0).soldeCrediteur()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultat.lignes().get(1).soldeCrediteur()).isEqualByComparingTo("100.00");
    }

    @Test
    void obtenirBalance_compteDeBilanAvecSoldeOuverture_reconcilieAvecLeSoldeDuGrandLivre() {
        // Même compte, même exercice, même solde d'ouverture (500.00) que
        // obtenirGrandLivre_compteDeBilan_utiliseSoldeAvantCommeOuverture : la balance doit
        // aboutir au même solde final que le grand livre pour ce compte.
        List<Object[]> mouvements = List.<Object[]>of(
                new Object[]{"compte-1", "411", "Clients", new BigDecimal("100.00"), BigDecimal.ZERO});
        when(ligneEcritureRepository.findBalance("ex-1", "entite-1")).thenReturn(mouvements);
        List<Object[]> ouvertures = List.<Object[]>of(
                new Object[]{"compte-1", "411", "Clients", new BigDecimal("500.00")});
        when(ligneEcritureRepository.calculerSoldesOuvertureBilan("entite-1", exercice.getDateDebut()))
                .thenReturn(ouvertures);

        BalanceReadDto resultat = service.obtenirBalance("kc-user", "ex-1");

        // solde = ouverture(500) + credit(0) - debit(100) = 400, positif => créditeur.
        assertThat(resultat.lignes().get(0).soldeCrediteur()).isEqualByComparingTo("400.00");
        assertThat(resultat.lignes().get(0).soldeDebiteur()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void obtenirBalance_compteDeBilanSansMouvementMaisAvecSoldeOuverture_apparaitQuandMeme() {
        when(ligneEcritureRepository.findBalance("ex-1", "entite-1")).thenReturn(List.of());
        List<Object[]> ouvertures = List.<Object[]>of(
                new Object[]{"compte-1", "411", "Clients", new BigDecimal("500.00")});
        when(ligneEcritureRepository.calculerSoldesOuvertureBilan("entite-1", exercice.getDateDebut()))
                .thenReturn(ouvertures);

        BalanceReadDto resultat = service.obtenirBalance("kc-user", "ex-1");

        assertThat(resultat.lignes()).hasSize(1);
        assertThat(resultat.lignes().get(0).soldeCrediteur()).isEqualByComparingTo("500.00");
        assertThat(resultat.lignes().get(0).totalDebit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultat.lignes().get(0).totalCredit()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
