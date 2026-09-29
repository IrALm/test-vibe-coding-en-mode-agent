package com.walsia.api_compta.moteurComptable.mapper;

import com.walsia.api_compta.moteurComptable.dto.readDto.ExerciceReadDto;
import com.walsia.api_compta.moteurComptable.entity.exercice.ExerciceComptable;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExerciceMapper {
    ExerciceReadDto toReadDto(ExerciceComptable exercice);
}
