package com.walsia.api_compta.integrationClient.mapper;

import com.walsia.api_compta.integrationClient.dto.readDto.TiersReadDto;
import com.walsia.api_compta.integrationClient.entity.tiers.Tiers;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TiersMapper {

    @Mapping(target = "compteAssocieId", source = "compteAssocie.id")
    @Mapping(target = "compteAssocieNumero", source = "compteAssocie.numero")
    @Mapping(target = "compteAssocieLibelle", source = "compteAssocie.libelle")
    TiersReadDto toReadDto(Tiers tiers);
}
