package com.walsia.api_compta.integrationClient.entity.referentiel;

/**
 * Référentiels comptables disponibles dans l'espace OHADA.
 * Chaque code détermine le plan de comptes applicable et les
 * modèles d'états financiers à générer.
 */
public enum CodeReferentiel {
    /** Système Normal : entreprises importantes, bilan + résultat + TFT + annexes complètes */
    SYSCOHADA_NORMAL,
    /** Système Minimal de Trésorerie : très petites entreprises */
    SYSCOHADA_SMT,
    /** Référentiel dédié aux associations, ONG et entités à but non lucratif */
    SYCEBNL
}
