package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.integrationClient.dto.formDto.EntiteCreationForm;
import com.walsia.api_compta.integrationClient.dto.readDto.EntiteCreeeReadDto;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.ReferentielComptable;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Role;
import com.walsia.api_compta.authentification.entity.UserTokenType;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.exception.ConflitException;
import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.readDto.EntiteReadDto;
import com.walsia.api_compta.integrationClient.mapper.EntiteMapper;
import com.walsia.api_compta.integrationClient.mapper.UtilisateurMapper;
import com.walsia.api_compta.integrationClient.repository.ClasseCompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.CompteComptableRepository;
import com.walsia.api_compta.integrationClient.repository.EntiteRepository;
import com.walsia.api_compta.integrationClient.repository.ReferentielComptableRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.integrationClient.service.interfaces.EntiteCreationService;
import com.walsia.api_compta.authentification.service.interfaces.KeycloakAdminService;
import com.walsia.api_compta.mail.service.interfaces.MailService;
import com.walsia.api_compta.authentification.service.interfaces.UserTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EntiteCreationServiceImpl implements EntiteCreationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CARACTERES_MOT_DE_PASSE =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private final EntiteRepository entiteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ReferentielComptableRepository referentielComptableRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final MailService mailService;
    private final UserTokenService userTokenService;
    private final EntiteMapper entiteMapper;
    private final UtilisateurMapper utilisateurMapper;
    private final ClasseCompteComptableRepository classeCompteComptableRepository;
    private final CompteComptableRepository compteComptableRepository;

    public EntiteCreationServiceImpl(
            EntiteRepository entiteRepository,
            UtilisateurRepository utilisateurRepository,
            ReferentielComptableRepository referentielComptableRepository,
            KeycloakAdminService keycloakAdminService,
            MailService mailService,
            UserTokenService userTokenService,
            EntiteMapper entiteMapper,
            UtilisateurMapper utilisateurMapper,
            ClasseCompteComptableRepository classeCompteComptableRepository,
            CompteComptableRepository compteComptableRepository) {
        this.entiteRepository = entiteRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.referentielComptableRepository = referentielComptableRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.mailService = mailService;
        this.userTokenService = userTokenService;
        this.entiteMapper = entiteMapper;
        this.utilisateurMapper = utilisateurMapper;
        this.classeCompteComptableRepository = classeCompteComptableRepository;
        this.compteComptableRepository = compteComptableRepository;
    }

    @Override
    @Transactional
    public EntiteCreeeReadDto creerEntiteEtAdmin(EntiteCreationForm form) {
        if (utilisateurRepository.existsByEmail(form.adminEmail())) {
            throw new ConflitException("Un utilisateur existe déjà avec cet email : " + form.adminEmail());
        }

        ReferentielComptable referentiel = referentielComptableRepository.findByCode(form.referentielComptableCode())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Référentiel comptable introuvable : " + form.referentielComptableCode()));

        Entite entite = entiteRepository.save(Entite.builder()
                .raisonSociale(form.raisonSociale())
                .typeEntite(form.typeEntite())
                .pays(form.pays())
                .devise(form.devise())
                .numeroIdentification(form.numeroIdentification())
                .dateCreation(LocalDate.now())
                .actif(true)
                .referentielComptable(referentiel)
                .build());

        String motDePasseTemporaire = genererMotDePasseTemporaire();

        // Créé en dehors de la transaction DB : en cas d'échec de la sauvegarde locale
        // qui suit, on compense manuellement en supprimant l'utilisateur Keycloak.
        String keycloakId = keycloakAdminService.creerUtilisateur(new KeycloakAdminService.NouvelUtilisateurKeycloak(
                form.adminEmail(),
                form.adminPrenom(),
                form.adminNom(),
                motDePasseTemporaire,
                List.of(Role.ADMIN.name())
        ));

        Utilisateur utilisateur;
        try {
            utilisateur = utilisateurRepository.save(Utilisateur.builder()
                    .nom(form.adminNom())
                    .postNom(form.adminPostNom())
                    .prenom(form.adminPrenom())
                    .email(form.adminEmail())
                    .role(Role.ADMIN)
                    .actif(true)
                    .keycloakId(keycloakId)
                    .emailVerifie(false)
                    .motDePasseTemporaire(true)
                    .entite(entite)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (RuntimeException e) {
            keycloakAdminService.supprimerUtilisateur(keycloakId);
            throw e;
        }

        mailService.envoyerMotDePasseTemporaire(form.adminEmail(), form.adminPrenom(), motDePasseTemporaire);
        String tokenVerification = userTokenService.genererToken(utilisateur, UserTokenType.EMAIL_VERIFICATION);
        mailService.envoyerEmailVerification(form.adminEmail(), form.adminPrenom(), tokenVerification);

        return new EntiteCreeeReadDto(avecCompteurs(entite), utilisateurMapper.toReadDto(utilisateur));
    }

    private String genererMotDePasseTemporaire() {
        StringBuilder motDePasse = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            motDePasse.append(CARACTERES_MOT_DE_PASSE.charAt(SECURE_RANDOM.nextInt(CARACTERES_MOT_DE_PASSE.length())));
        }
        return motDePasse.toString();
    }

    /** Complète l'EntiteReadDto avec les compteurs du référentiel (classes/comptes), non portés par le mapper. */
    private EntiteReadDto avecCompteurs(Entite entite) {
        EntiteReadDto base = entiteMapper.toReadDto(entite);
        String referentielId = entite.getReferentielComptable().getId();
        long nombreClasses = classeCompteComptableRepository.countByReferentielComptable_Id(referentielId);
        long nombreComptes = compteComptableRepository.countByClasseCompteComptable_ReferentielComptable_Id(referentielId);
        return new EntiteReadDto(
                base.id(), base.raisonSociale(), base.typeEntite(), base.pays(), base.devise(),
                base.numeroIdentification(), base.dateCreation(), base.actif(),
                base.referentielComptableCode(), base.referentielComptableLibelle(),
                nombreClasses, nombreComptes);
    }
}
