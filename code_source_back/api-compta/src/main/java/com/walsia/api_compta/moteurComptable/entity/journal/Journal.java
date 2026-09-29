package com.walsia.api_compta.moteurComptable.entity.journal;

import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import jakarta.persistence.*;
import lombok.*;

/**
 * Journal comptable d'une entité. Phase 1 : un seul journal ("Journal général") par
 * entité, provisionné paresseusement au premier exercice/écriture. La contrainte
 * unique sur entite_id impose ce singleton ; sa suppression suffira à ouvrir la voie
 * aux journaux auxiliaires (achats, ventes, banque, caisse, OD) en Phase 2.
 */
@Entity
@Table(name = "journal", uniqueConstraints = @UniqueConstraint(columnNames = "entite_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Journal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String libelle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entite_id", nullable = false)
    private Entite entite;

    /** Compteur de séquence du journal, incrémenté atomiquement (verrou) à chaque validation d'écriture. */
    @Column(name = "dernier_numero", nullable = false)
    private int dernierNumero;
}
