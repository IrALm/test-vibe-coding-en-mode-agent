package com.walsia.api_compta.integrationClient.entity.tiers;

import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import jakarta.persistence.*;
import lombok.*;

/**
 * Fiche Tiers unique (client, fournisseur, salarié...) qui centralise à la
 * fois les informations administratives (contact, adresse, contrat) et le
 * rattachement comptable (compte collectif 401x/411x pour le lettrage).
 * C'est la jonction entre le volet administratif et le volet financier :
 * une facture créée pour ce tiers générera une écriture sur son compte associé.
 */
@Entity
@Table(name = "tiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tiers {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeTiers type;

    @Column(nullable = false)
    private String raisonSociale;

    private String nomContact;
    private String email;
    private String telephone;
    private String adresse;

    /** Numéro d'identification fiscale / registre de commerce du tiers */
    private String numeroFiscal;

    /** Intitulé du poste, pertinent uniquement si type = SALARIE */
    private String intitulePoste;

    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entite_id", nullable = false)
    private Entite entite;

    /**
     * Compte comptable collectif associé (ex : 411 pour un client, 401 pour
     * un fournisseur). Permet de générer automatiquement les écritures liées
     * aux factures et de faire le lettrage.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_associe_id")
    private CompteComptable compteAssocie;
}
