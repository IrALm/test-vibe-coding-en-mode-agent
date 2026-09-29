package com.walsia.api_compta.integrationClient.entity.referentiel;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Version officielle et datée du plan de comptes d'un ReferentielComptable
 * (ex : SYSCOHADA Normal révisé 2018), partagée par toutes les Entite qui
 * utilisent ce référentiel. Les comptes personnalisés d'une entreprise sont
 * portés par CompteComptable.entite, pas par une duplication de PlanComptable.
 */
@Entity
@Table(name = "plan_comptable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referentiel_comptable_id", nullable = false)
    private ReferentielComptable referentielComptable;

    private LocalDate dateMiseEnPlace;

    private boolean actif = true;

    @Builder.Default
    @OneToMany(mappedBy = "planComptable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompteComptable> comptes = new ArrayList<>();
}
