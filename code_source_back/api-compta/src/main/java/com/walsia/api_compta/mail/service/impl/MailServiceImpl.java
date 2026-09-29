package com.walsia.api_compta.mail.service.impl;

import com.walsia.api_compta.mail.service.interfaces.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Envoi des emails via la Hostinger Mail API (REST, Bearer token) :
 * POST /api/v1/mailboxes/{mailboxResourceId}/send.
 * mailboxResourceId est l'identifiant de ressource de la mailbox côté
 * Hostinger (PAS l'adresse email) - le token doit être autorisé pour cette
 * mailbox précise.
 */
@Service
public class MailServiceImpl implements MailService {

    private final RestClient restClient;
    private final String mailboxResourceId;
    private final String displayName;
    private final String frontendBaseUrl;

    public MailServiceImpl(
            @Value("${app.mail.api-base-url}") String apiBaseUrl,
            @Value("${app.mail.api-token}") String apiToken,
            @Value("${app.mail.mailbox-resource-id}") String mailboxResourceId,
            @Value("${app.mail.display-name}") String displayName,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .build();
        this.mailboxResourceId = mailboxResourceId;
        this.displayName = displayName;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void envoyerMotDePasseTemporaire(String destinataire, String prenom, String motDePasseTemporaire) {
        String contenu = """
                <p>Bonjour %s,</p>
                <p>Votre compte administrateur a été créé sur l'ERP Comptable.</p>
                <p>Votre mot de passe temporaire est : <strong>%s</strong></p>
                <p>Il vous sera demandé de le changer lors de votre première connexion.</p>
                """.formatted(prenom, motDePasseTemporaire);
        envoyer(destinataire, "Bienvenue sur l'ERP Comptable - vos identifiants", contenu);
    }

    @Override
    public void envoyerEmailVerification(String destinataire, String prenom, String token) {
        String lien = frontendBaseUrl + "/verify-email?token=" + token;
        String contenu = """
                <p>Bonjour %s,</p>
                <p>Merci de confirmer votre adresse email en cliquant sur le lien ci-dessous :</p>
                <p><a href="%s">Vérifier mon email</a></p>
                <p>Ce lien expire dans 24 heures.</p>
                """.formatted(prenom, lien);
        envoyer(destinataire, "Vérifiez votre adresse email", contenu);
    }

    @Override
    public void envoyerReinitialisationMotDePasse(String destinataire, String prenom, String token) {
        String lien = frontendBaseUrl + "/reset-password?token=" + token;
        String contenu = """
                <p>Bonjour %s,</p>
                <p>Une demande de réinitialisation de mot de passe a été effectuée pour votre compte.</p>
                <p><a href="%s">Réinitialiser mon mot de passe</a></p>
                <p>Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>
                <p>Ce lien expire dans 1 heure.</p>
                """.formatted(prenom, lien);
        envoyer(destinataire, "Réinitialisation de votre mot de passe", contenu);
    }

    private void envoyer(String destinataire, String sujet, String contenuHtml) {
        Map<String, Object> body = Map.of(
                "to", List.of(destinataire),
                "displayName", displayName,
                "subject", sujet,
                "html", contenuHtml
        );

        restClient.post()
                .uri("/api/v1/mailboxes/{mailboxResourceId}/send", mailboxResourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
