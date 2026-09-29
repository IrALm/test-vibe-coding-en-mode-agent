package com.walsia.api_compta.integrationClient.dto.formDto;

/** compteAssocieId = null -> dissociation explicite. Endpoint dédié : pas d'ambiguïté "champ absent" vs "null". */
public record TiersAssociationCompteForm(
        String compteAssocieId
) {
}
