package com.walsia.api_compta.moteurComptable.dto.readDto;

import com.walsia.api_compta.moteurComptable.entity.exercice.StatutExercice;

import java.time.LocalDate;

public record ExerciceReadDto(String id, LocalDate dateDebut, LocalDate dateFin, StatutExercice statut) {
}
