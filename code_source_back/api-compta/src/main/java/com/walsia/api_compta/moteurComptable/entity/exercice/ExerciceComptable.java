package com.walsia.api_compta.moteurComptable.entity.exercice;

import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Exercice comptable d'une entité. Un seul exercice OUVERT à la fois par entité
 * (imposé par un index unique partiel en base, cf. V19). Phase 1 : pas de réouverture
 * d'un exercice CLOS.
 */
@Entity
@Table(name = "exercice_comptable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciceComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatutExercice statut;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entite_id", nullable = false)
    private Entite entite;
}
