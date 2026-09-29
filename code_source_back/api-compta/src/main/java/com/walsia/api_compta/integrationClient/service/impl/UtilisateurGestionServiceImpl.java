package com.walsia.api_compta.integrationClient.service.impl;

import com.walsia.api_compta.authentification.entity.UserTokenType;
import com.walsia.api_compta.authentification.service.interfaces.KeycloakAdminService;
import com.walsia.api_compta.authentification.service.interfaces.UserTokenService;
import com.walsia.api_compta.exception.ConflitException;
import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.integrationClient.dto.formDto.UtilisateurCreationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.UtilisateurSearchForm;
import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurPageReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurReadDto;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Role;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.integrationClient.mapper.UtilisateurMapper;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.integrationClient.service.interfaces.UtilisateurGestionService;
import com.walsia.api_compta.integrationClient.specification.UtilisateurSpecification;
import com.walsia.api_compta.mail.service.interfaces.MailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UtilisateurGestionServiceImpl implements UtilisateurGestionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CARACTERES_MOT_DE_PASSE =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";

    private final UtilisateurRepository utilisateurRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final MailService mailService;
    private final UserTokenService userTokenService;
    private final UtilisateurMapper utilisateurMapper;
    private final UtilisateurSpecification utilisateurSpecification;

    public UtilisateurGestionServiceImpl(
            UtilisateurRepository utilisateurRepository,
            KeycloakAdminService keycloakAdminService,
            MailService mailService,
            UserTokenService userTokenService,
            UtilisateurMapper utilisateurMapper,
            UtilisateurSpecification utilisateurSpecification) {
        this.utilisateurRepository = utilisateurRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.mailService = mailService;
        this.userTokenService = userTokenService;
        this.utilisateurMapper = utilisateurMapper;
        this.utilisateurSpecification = utilisateurSpecification;
    }

    @Override
    @Transactional
    public UtilisateurReadDto ajouterUtilisateur(String keycloakIdAppelant, UtilisateurCreationForm form) {
        Entite entite = entiteAppelant(keycloakIdAppelant);

        if (utilisateurRepository.existsByEmail(form.email())) {
            throw new ConflitException("Un utilisateur existe déjà avec cet email : " + form.email());
        }

        String motDePasseTemporaire = genererMotDePasseTemporaire();

        // Créé en dehors de la transaction DB : en cas d'échec de la sauvegarde locale
        // qui suit, on compense manuellement en supprimant l'utilisateur Keycloak.
        String keycloakId = keycloakAdminService.creerUtilisateur(new KeycloakAdminService.NouvelUtilisateurKeycloak(
                form.email(),
                form.prenom(),
                form.nom(),
                motDePasseTemporaire,
                List.of(form.role().name())
        ));

        Utilisateur utilisateur;
        try {
            utilisateur = utilisateurRepository.save(Utilisateur.builder()
                    .nom(form.nom())
                    .postNom(form.postNom())
                    .prenom(form.prenom())
                    .email(form.email())
                    .role(form.role())
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

        mailService.envoyerMotDePasseTemporaire(form.email(), form.prenom(), motDePasseTemporaire);
        String tokenVerification = userTokenService.genererToken(utilisateur, UserTokenType.EMAIL_VERIFICATION);
        mailService.envoyerEmailVerification(form.email(), form.prenom(), tokenVerification);

        return utilisateurMapper.toReadDto(utilisateur);
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurPageReadDto rechercherUtilisateurs(String keycloakIdAppelant, UtilisateurSearchForm form) {
        Entite entite = entiteAppelant(keycloakIdAppelant);

        Pageable pageable = PageRequest.of(form.page(), form.size(), Sort.by(form.sortDirection(), form.sortBy()));
        Page<UtilisateurReadDto> page = utilisateurRepository
                .findAll(utilisateurSpecification.build(form, entite.getId()), pageable)
                .map(utilisateurMapper::toReadDto);

        return UtilisateurPageReadDto.from(page);
    }

    @Override
    @Transactional
    public UtilisateurReadDto activer(String keycloakIdAppelant, String utilisateurId) {
        Utilisateur utilisateur = utilisateurDeLEntite(keycloakIdAppelant, utilisateurId);
        utilisateur.setActif(true);
        utilisateurRepository.save(utilisateur);
        return utilisateurMapper.toReadDto(utilisateur);
    }

    @Override
    @Transactional
    public UtilisateurReadDto desactiver(String keycloakIdAppelant, String utilisateurId) {
        Utilisateur utilisateur = utilisateurDeLEntite(keycloakIdAppelant, utilisateurId);

        if (utilisateur.getRole() == Role.ADMIN && utilisateur.isActif()) {
            long adminsActifs = utilisateurRepository.countByEntite_IdAndRoleAndActif(
                    utilisateur.getEntite().getId(), Role.ADMIN, true);
            if (adminsActifs <= 1) {
                throw new ConflitException("Impossible de désactiver le dernier administrateur actif de l'entreprise");
            }
        }

        utilisateur.setActif(false);
        utilisateurRepository.save(utilisateur);
        return utilisateurMapper.toReadDto(utilisateur);
    }

    private Entite entiteAppelant(String keycloakIdAppelant) {
        return utilisateurRepository.findByKeycloakId(keycloakIdAppelant)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"))
                .getEntite();
    }

    /** Charge l'utilisateur ciblé et vérifie son appartenance à l'entreprise de l'appelant - 404 sinon, pour ne pas confirmer l'existence d'un utilisateur d'une autre entreprise. */
    private Utilisateur utilisateurDeLEntite(String keycloakIdAppelant, String utilisateurId) {
        Entite entite = entiteAppelant(keycloakIdAppelant);
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
        if (!utilisateur.getEntite().getId().equals(entite.getId())) {
            throw new RessourceIntrouvableException("Utilisateur introuvable");
        }
        return utilisateur;
    }

    private String genererMotDePasseTemporaire() {
        StringBuilder motDePasse = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            motDePasse.append(CARACTERES_MOT_DE_PASSE.charAt(SECURE_RANDOM.nextInt(CARACTERES_MOT_DE_PASSE.length())));
        }
        return motDePasse.toString();
    }
}
