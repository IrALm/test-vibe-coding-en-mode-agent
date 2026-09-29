package com.walsia.api_compta.integrationClient.entity.entite;

import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.ReferentielComptable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Une "Entite" représente la structure gérée par l'application :
 * une PME, une ONG, une association, etc.
 * L'application est pensée multi-structures : chaque Entite a son
 * propre référentiel comptable, son propre plan de comptes, ses propres
 * tiers, exercices et utilisateurs.
 */
@Entity
@Table(name = "entite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String raisonSociale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeEntite typeEntite;

    /** Pays d'implantation (zone OHADA), utile pour les spécificités locales */
    private String pays;

    /** Devise principale de l'entité (XOF, XAF, EUR...) */
    @Column(length = 3)
    private String devise;

    /** Numéro d'identification fiscale / registre de commerce */
    private String numeroIdentification;

    private LocalDate dateCreation;

    private boolean actif = true;

    /**
     * Référentiel comptable actif pour cette entité.
     * Détermine le plan de comptes et les modèles d'états financiers utilisés.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referentiel_comptable_id", nullable = false)
    private ReferentielComptable referentielComptable;

    @Builder.Default
    @OneToMany(mappedBy = "entite", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompteComptable> comptes = new ArrayList<>();
}
