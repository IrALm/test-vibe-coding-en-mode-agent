package com.walsia.api_compta.integrationClient.entity.entite;

/**
 * Type de structure gérée par l'application.
 * Détermine, entre autres, le référentiel comptable par défaut
 * (SYSCOHADA Normal pour une PME, SYCEBNL pour une ONG, etc.).
 */
public enum TypeEntite {
    PME,
    ONG,
    ASSOCIATION,
    ECOLE,
    AUTRE
}
