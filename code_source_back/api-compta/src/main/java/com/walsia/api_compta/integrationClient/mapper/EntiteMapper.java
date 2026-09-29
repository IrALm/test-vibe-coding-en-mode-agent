package com.walsia.api_compta.integrationClient.mapper;

import com.walsia.api_compta.integrationClient.dto.readDto.EntiteReadDto;
import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EntiteMapper {

    @Mapping(target = "referentielComptableCode", source = "referentielComptable.code")
    @Mapping(target = "referentielComptableLibelle", source = "referentielComptable.libelle")
    @Mapping(target = "nombreClasses", ignore = true)
    @Mapping(target = "nombreComptes", ignore = true)
    EntiteReadDto toReadDto(Entite entite);
}
