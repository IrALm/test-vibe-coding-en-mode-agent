package com.walsia.api_compta.integrationClient.mapper;

import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurReadDto;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UtilisateurMapper {

    @Mapping(target = "entiteId", source = "entite.id")
    UtilisateurReadDto toReadDto(Utilisateur utilisateur);
}
