package com.walsia.api_compta.integrationClient.mapper;

import com.walsia.api_compta.integrationClient.dto.readDto.ClasseCompteComptableReadDto;
import com.walsia.api_compta.integrationClient.entity.referentiel.ClasseCompteComptable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClasseCompteComptableMapper {

    @Mapping(target = "nombreComptes", ignore = true)
    ClasseCompteComptableReadDto toReadDto(ClasseCompteComptable classe);
}
