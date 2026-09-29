package com.walsia.api_compta.mail.service.interfaces;

public interface MailService {

    void envoyerMotDePasseTemporaire(String destinataire, String prenom, String motDePasseTemporaire);

    void envoyerEmailVerification(String destinataire, String prenom, String token);

    void envoyerReinitialisationMotDePasse(String destinataire, String prenom, String token);
}
