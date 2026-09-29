package com.walsia.api_compta.moteurComptable.entity.ecriture;

import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.moteurComptable.entity.exercice.ExerciceComptable;
import com.walsia.api_compta.moteurComptable.entity.journal.Journal;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Écriture comptable en partie double. Immuable une fois VALIDEE (§1.3 cahier des
 * charges) : toute correction passe par une contre-passation (nouvelle écriture miroir),
 * jamais par édition/suppression directe. Le numéro séquentiel n'est assigné qu'à la
 * validation (jamais à la création du brouillon) pour ne jamais laisser de trou dans la
 * numérotation légale du journal.
 */
@Entity
@Table(name = "ecriture", uniqueConstraints = @UniqueConstraint(columnNames = {"journal_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ecriture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "date_ecriture", nullable = false)
    private LocalDate date;

    private String reference;

    @Column(nullable = false)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatutEcriture statut;

    @Column(length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_id", nullable = false)
    private Journal journal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercice_id", nullable = false)
    private ExerciceComptable exercice;

    /** Dénormalisé depuis exercice.entite (symétrie de scoping tenant avec Tiers/CompteComptable). Jamais fourni par le client. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entite_id", nullable = false)
    private Entite entite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cree_par_id", nullable = false)
    private Utilisateur creePar;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @Column(name = "valide_le")
    private LocalDateTime valideLe;

    @Column(name = "motif_rejet", length = 500)
    private String motifRejet;

    /** Renseigné sur l'ORIGINALE une fois que son miroir (contre-passation) devient VALIDEE. */
    @Column(name = "ecriture_miroir_id")
    private String ecritureMiroirId;

    /** Renseigné sur le MIROIR dès sa création : id de l'écriture qu'il contre-passe. */
    @Column(name = "contre_passation_de_id")
    private String contrePassationDeId;

    /** Écriture système générée par ExerciceComptableServiceImpl.cloturerExercice() (solde des
     * comptes de gestion 6/7/8 vers le compte de résultat 111/119) - exclue de calculerResultat
     * pour ne pas s'annuler elle-même dans le résultat de l'exercice qu'elle clôture. */
    @Builder.Default
    @Column(name = "genere_par_cloture", nullable = false)
    private boolean genereParCloture = false;

    @Builder.Default
    @OneToMany(mappedBy = "ecriture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneEcriture> lignes = new ArrayList<>();
}
