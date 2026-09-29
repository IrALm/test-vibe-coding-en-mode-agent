package com.walsia.api_compta.integrationClient.entity.utilisateur;

/** Whitelist des champs triables pour la recherche d'utilisateurs (évite d'exposer un tri sur un champ arbitraire). */
public enum UtilisateurSortEnum {

    NOM("nom"),
    EMAIL("email"),
    ROLE("role"),
    CREATED_AT("createdAt");

    private final String champ;

    UtilisateurSortEnum(String champ) {
        this.champ = champ;
    }

    /** Résout une valeur libre (nom d'enum ou nom de champ) vers un nom de champ sûr ; retombe sur CREATED_AT si absent/invalide. */
    public static String resolveField(String valeur) {
        if (valeur != null) {
            for (UtilisateurSortEnum sort : values()) {
                if (sort.name().equalsIgnoreCase(valeur) || sort.champ.equalsIgnoreCase(valeur)) {
                    return sort.champ;
                }
            }
        }
        return CREATED_AT.champ;
    }
}
