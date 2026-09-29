package com.walsia.api_compta.integrationClient.mapper;

import com.walsia.api_compta.integrationClient.dto.readDto.CompteComptableReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.CompteOptionReadDto;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompteComptableMapper {

    CompteComptableReadDto toReadDto(CompteComptable compte);

    CompteOptionReadDto toOptionReadDto(CompteComptable compte);
}
