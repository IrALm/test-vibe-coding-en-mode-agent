package com.walsia.api_compta.integrationClient.entity.referentiel;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Décrit un cadre normatif OHADA (SYSCOHADA Normal, SMT, SYCEBNL...).
 * C'est une donnée de référence partagée : elle n'est pas dupliquée
 * par entité, seul le PlanComptable qui en découle l'est.
 */
@Entity
@Table(name = "referentiel_comptable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferentielComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 30)
    private CodeReferentiel code;

    @Column(nullable = false)
    private String libelle;

    private String description;

    /** Ex : "Révisé 2018" — utile pour tracer les évolutions du référentiel */
    private String version;

    @Builder.Default
    @OneToMany(mappedBy = "referentielComptable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanComptable> plansComptables = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "referentielComptable", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ClasseCompteComptable> classeCompteComptableSet = new HashSet<>();
}
