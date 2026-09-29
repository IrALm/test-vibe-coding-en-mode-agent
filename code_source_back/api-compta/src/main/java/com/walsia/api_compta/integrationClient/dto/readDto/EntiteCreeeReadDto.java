package com.walsia.api_compta.integrationClient.dto.readDto;

/** Réponse retournée après création d'une entité : l'entité et son administrateur créé. */
public record EntiteCreeeReadDto(
        EntiteReadDto entite,
        UtilisateurReadDto administrateur
) {
}
