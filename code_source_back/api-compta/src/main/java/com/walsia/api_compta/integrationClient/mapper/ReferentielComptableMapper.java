package com.walsia.api_compta.integrationClient.mapper;

import com.walsia.api_compta.integrationClient.dto.readDto.ReferentielComptableReadDto;
import com.walsia.api_compta.integrationClient.entity.referentiel.ReferentielComptable;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReferentielComptableMapper {

    ReferentielComptableReadDto toReadDto(ReferentielComptable referentielComptable);
}
