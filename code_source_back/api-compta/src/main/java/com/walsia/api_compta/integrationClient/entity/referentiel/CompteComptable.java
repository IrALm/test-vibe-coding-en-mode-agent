package com.walsia.api_compta.integrationClient.entity.referentiel;

import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import jakarta.persistence.*;
import lombok.*;

/**
 * Un compte du plan comptable SYSCOHADA (ex : 411 "Clients", 512 "Banques").
 * La codification décimale est respectée : le compteParent permet de
 * remonter la hiérarchie (ex : 4111 -> 411 -> 41 -> classe 4).
 */
@Entity
@Table(
    name = "compte_comptable",
    uniqueConstraints = @UniqueConstraint(columnNames = {"plan_comptable_id", "numero"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompteComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Numéro de compte selon la codification décimale SYSCOHADA (ex : "411", "4111") */
    @Column(nullable = false, length = 10)
    private String numero;

    @Column(nullable = false)
    private String libelle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classe_compte_comptable_id", nullable = false)
    private ClasseCompteComptable classeCompteComptable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SensCompte sensNormal;

    private boolean lettrable = false;

    /** @Builder.Default : sans lui, Lombok ignore l'initialiseur "= true" dans le builder et
     * produit un compte inactif par défaut dès qu'un appelant omet .actif(...) explicitement. */
    @Builder.Default
    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_comptable_id", nullable = false)
    private PlanComptable planComptable;

    /** Compte parent dans la hiérarchie décimale (null pour un compte racine) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_parent_id")
    private CompteComptable compteParent;

    /** Utile pour les sous-comptes spécifques crée par une entreprise : sinon reste à
     * false pour les comptes standards
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entite_id")
    private Entite entite;
}
