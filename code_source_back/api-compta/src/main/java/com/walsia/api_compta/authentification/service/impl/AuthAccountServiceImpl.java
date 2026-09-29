package com.walsia.api_compta.authentification.service.impl;

import com.walsia.api_compta.authentification.entity.UserTokenType;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.exception.ConflitException;
import com.walsia.api_compta.exception.RessourceIntrouvableException;
import com.walsia.api_compta.authentification.repository.UserSessionRepository;
import com.walsia.api_compta.integrationClient.repository.UtilisateurRepository;
import com.walsia.api_compta.authentification.service.interfaces.AuthAccountService;
import com.walsia.api_compta.authentification.service.interfaces.KeycloakAdminService;
import com.walsia.api_compta.mail.service.interfaces.MailService;
import com.walsia.api_compta.authentification.service.interfaces.UserTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthAccountServiceImpl implements AuthAccountService {

    private final UtilisateurRepository utilisateurRepository;
    private final UserSessionRepository userSessionRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final MailService mailService;
    private final UserTokenService userTokenService;

    public AuthAccountServiceImpl(
            UtilisateurRepository utilisateurRepository,
            UserSessionRepository userSessionRepository,
            KeycloakAdminService keycloakAdminService,
            MailService mailService,
            UserTokenService userTokenService) {
        this.utilisateurRepository = utilisateurRepository;
        this.userSessionRepository = userSessionRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.mailService = mailService;
        this.userTokenService = userTokenService;
    }

    @Override
    public void demanderReinitialisationMotDePasse(String email) {
        // Silencieux si l'email est inconnu : évite de révéler l'existence d'un compte.
        utilisateurRepository.findByEmail(email).ifPresent(utilisateur -> {
            String token = userTokenService.genererToken(utilisateur, UserTokenType.PASSWORD_RESET);
            mailService.envoyerReinitialisationMotDePasse(utilisateur.getEmail(), utilisateur.getNom(), token);
        });
    }

    @Override
    @Transactional
    public void reinitialiserMotDePasse(String token, String nouveauMotDePasse) {
        Utilisateur utilisateur = userTokenService.consommerToken(token, UserTokenType.PASSWORD_RESET);
        keycloakAdminService.reinitialiserMotDePasse(utilisateur.getKeycloakId(), nouveauMotDePasse);

        // Un mot de passe compromis puis réinitialisé ne doit pas laisser une session active tourner.
        keycloakAdminService.deconnecterToutesLesSessions(utilisateur.getKeycloakId());
        LocalDateTime maintenant = LocalDateTime.now();
        var sessionsActives = userSessionRepository.findAllByUtilisateur_IdAndRevokedAtIsNull(utilisateur.getId());
        sessionsActives.forEach(session -> session.setRevokedAt(maintenant));
        userSessionRepository.saveAll(sessionsActives);
    }

    @Override
    @Transactional
    public void verifierEmail(String token) {
        Utilisateur utilisateur = userTokenService.consommerToken(token, UserTokenType.EMAIL_VERIFICATION);
        utilisateur.setEmailVerifie(true);
        utilisateurRepository.save(utilisateur);
        keycloakAdminService.marquerEmailVerifie(utilisateur.getKeycloakId(), true);
    }

    @Override
    public void renvoyerEmailVerification(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + email));
        if (utilisateur.isEmailVerifie()) {
            throw new ConflitException("Cet email est déjà vérifié");
        }
        String token = userTokenService.genererToken(utilisateur, UserTokenType.EMAIL_VERIFICATION);
        mailService.envoyerEmailVerification(utilisateur.getEmail(), utilisateur.getNom(), token);
    }

    @Override
    @Transactional
    public void definirMotDePassePermanent(String keycloakId, String nouveauMotDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
        keycloakAdminService.reinitialiserMotDePasse(keycloakId, nouveauMotDePasse);
        utilisateur.setMotDePasseTemporaire(false);
        utilisateurRepository.save(utilisateur);
    }
}
