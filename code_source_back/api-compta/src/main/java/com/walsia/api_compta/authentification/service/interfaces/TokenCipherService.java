package com.walsia.api_compta.authentification.service.interfaces;

/**
 * Chiffrement réversible (AES-GCM) des tokens Keycloak stockés dans UserSession.
 * Contrairement à UserTokenService (hash irréversible, usage unique), ces tokens
 * doivent pouvoir être déchiffrés pour être retransmis à Keycloak (refresh, logout).
 */
public interface TokenCipherService {

    String chiffrer(String valeurEnClair);

    String dechiffrer(String valeurChiffree);
}
