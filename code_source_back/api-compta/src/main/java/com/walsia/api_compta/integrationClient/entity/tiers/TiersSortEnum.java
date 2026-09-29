package com.walsia.api_compta.integrationClient.entity.tiers;

import org.springframework.data.domain.Sort;

/** Whitelist des tris exposés pour la recherche de tiers (contrat figé côté front : raison,asc / raison,desc / type / statut). */
public enum TiersSortEnum {

    RAISON_ASC("raison,asc", "raisonSociale", Sort.Direction.ASC),
    RAISON_DESC("raison,desc", "raisonSociale", Sort.Direction.DESC),
    TYPE("type", "type", Sort.Direction.ASC),
    STATUT("statut", "actif", Sort.Direction.DESC);

    private final String valeur;
    private final String champ;
    private final Sort.Direction direction;

    TiersSortEnum(String valeur, String champ, Sort.Direction direction) {
        this.valeur = valeur;
        this.champ = champ;
        this.direction = direction;
    }

    /** Résout une valeur libre de query param vers un tri sûr ; retombe sur raisonSociale ASC si absent/invalide. */
    public static Sort resolve(String valeur) {
        if (valeur != null) {
            for (TiersSortEnum sort : values()) {
                if (sort.valeur.equalsIgnoreCase(valeur)) {
                    return Sort.by(sort.direction, sort.champ);
                }
            }
        }
        return Sort.by(Sort.Direction.ASC, "raisonSociale");
    }
}
