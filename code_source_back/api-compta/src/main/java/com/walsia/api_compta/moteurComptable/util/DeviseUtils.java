package com.walsia.api_compta.moteurComptable.util;

/**
 * Nombre de décimales usuelles par devise, pour le formatage côté lecture uniquement.
 * Le montant persisté (NUMERIC(19,2)) n'est jamais arrondi côté back quelle que soit
 * la devise de l'entreprise : c'est un problème d'affichage, pas de stockage.
 */
public final class DeviseUtils {

    private DeviseUtils() {
    }

    public static int decimales(String devise) {
        return "CDF".equalsIgnoreCase(devise) ? 0 : 2;
    }
}
