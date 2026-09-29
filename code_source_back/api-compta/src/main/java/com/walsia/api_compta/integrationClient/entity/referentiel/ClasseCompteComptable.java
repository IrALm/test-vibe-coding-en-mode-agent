package com.walsia.api_compta.integrationClient.entity.referentiel;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe comptable du référentiel (ex : SYSCOHADA (1 à 9), classe 4 "Comptes de tiers").
 * Regroupe les comptes de même nature au premier niveau de la codification décimale.
 */
@Entity
@Table(
    name = "classe_compte_comptable",
    uniqueConstraints = @UniqueConstraint(columnNames = {"referentiel_comptable_id", "numero"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClasseCompteComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private String titre;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referentiel_comptable_id", nullable = false)
    private ReferentielComptable referentielComptable;

    @Builder.Default
    @OneToMany(mappedBy = "classeCompteComptable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompteComptable> comptes = new ArrayList<>();
}
