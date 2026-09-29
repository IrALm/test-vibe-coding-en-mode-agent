package com.walsia.compta.tenant.domain;

/**
 * Cycle de vie d'un tenant.
 * <p>
 * Remarque : un statut SUSPENDED est envisagé pour un lot futur (suspension
 * d'un tenant actif) mais n'est volontairement pas implémenté dans ce lot,
 * qui ne couvre que le provisioning initial.
 */
public enum TenantStatus {
    PROVISIONING,
    ACTIVE,
    FAILED
}
