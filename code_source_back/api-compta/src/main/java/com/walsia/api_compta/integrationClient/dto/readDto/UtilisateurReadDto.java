package com.walsia.api_compta.integrationClient.dto.readDto;

import com.walsia.api_compta.integrationClient.entity.utilisateur.Role;

import java.time.LocalDateTime;

public record UtilisateurReadDto(
        String id,
        String nom,
        String postNom,
        String prenom,
        String email,
        Role role,
        boolean actif,
        boolean emailVerifie,
        boolean motDePasseTemporaire,
        String entiteId,
        LocalDateTime createdAt
) {
}
