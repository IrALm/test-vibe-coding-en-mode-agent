package com.walsia.api_compta.integrationClient.specification;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Normalisation de texte pour les recherches insensibles à la casse et aux accents (voir unaccent() côté SQL, migration V16). */
public final class RechercheTexteUtils {

    private static final Pattern DIACRITIQUES = Pattern.compile("\\p{M}");

    private RechercheTexteUtils() {
    }

    public static String normaliser(String valeur) {
        String sansAccents = Normalizer.normalize(valeur, Normalizer.Form.NFD);
        return DIACRITIQUES.matcher(sansAccents).replaceAll("").toLowerCase();
    }
}
