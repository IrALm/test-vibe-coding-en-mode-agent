package com.walsia.api_compta.integrationClient.dto.readDto;

import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;

/** Projection légère d'un compte, pour alimenter un select (ex. « Compte parent » du formulaire de création).
 * sensNormal permet au front de verrouiller le sens du sous-compte sur celui du parent choisi. */
public record CompteOptionReadDto(
        String id,
        String numero,
        String libelle,
        SensCompte sensNormal
) {
}
