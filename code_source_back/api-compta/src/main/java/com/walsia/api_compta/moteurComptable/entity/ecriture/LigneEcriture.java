package com.walsia.api_compta.moteurComptable.entity.ecriture;

import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ligne_ecriture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneEcriture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ecriture_id", nullable = false)
    private Ecriture ecriture;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_id", nullable = false)
    private CompteComptable compte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SensCompte sens;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    private String libelle;
}
