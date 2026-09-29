package com.walsia.api_compta.integrationClient.entity.utilisateur;

import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Utilisateur de l'application, rattaché à une Entite (PME, ONG...).
 * Le rôle détermine les droits d'accès aux modules administratif et financier.
 */
@Entity
@Table(name = "utilisateur", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "post_nom")
    private String postNom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    private boolean actif = true;

    /** Identifiant de l'utilisateur dans Keycloak (realm erp-comptable), source de vérité des identifiants/mot de passe. */
    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;

    @Column(name = "email_verifie", nullable = false)
    private boolean emailVerifie = false;

    /** Vrai tant que l'utilisateur n'a pas défini son mot de passe définitif (post-création). */
    @Column(name = "mot_de_passe_temporaire", nullable = false)
    private boolean motDePasseTemporaire = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entite_id", nullable = false)
    private Entite entite;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
