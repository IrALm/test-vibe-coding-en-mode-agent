package com.walsia.api_compta.authentification.entity;

import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Session de login backend-mediated (BFF). Seul le hash du token de session
 * (posé dans le cookie httpOnly) est stocké ; les tokens Keycloak sont
 * chiffrés (réversible, contrairement à UserToken) car ils doivent être
 * retransmis à Keycloak pour rafraîchir/révoquer la session.
 */
@Entity
@Table(name = "user_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "session_token_hash", nullable = false, unique = true)
    private String sessionTokenHash;

    @Column(name = "access_token_chiffre", nullable = false, columnDefinition = "TEXT")
    private String accessTokenChiffre;

    @Column(name = "access_token_expires_at", nullable = false)
    private LocalDateTime accessTokenExpiresAt;

    @Column(name = "refresh_token_chiffre", nullable = false, columnDefinition = "TEXT")
    private String refreshTokenChiffre;

    @Column(name = "refresh_token_expires_at", nullable = false)
    private LocalDateTime refreshTokenExpiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
