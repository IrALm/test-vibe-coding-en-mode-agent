package com.walsia.api_compta.moteurComptable.service.interfaces;

import com.walsia.api_compta.moteurComptable.dto.formDto.ExerciceCreationForm;
import com.walsia.api_compta.moteurComptable.dto.readDto.ClotureCheckReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.ExerciceReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.ResultatReadDto;

import java.util.List;

public interface ExerciceComptableService {

    List<ExerciceReadDto> listerExercices(String keycloakIdAppelant);

    ExerciceReadDto obtenirExerciceOuvert(String keycloakIdAppelant);

    ResultatReadDto obtenirResultat(String keycloakIdAppelant, String exerciceId);

    ExerciceReadDto creerExercice(String keycloakIdAppelant, ExerciceCreationForm form);

    ClotureCheckReadDto verifierCloture(String keycloakIdAppelant, String exerciceId);

    ExerciceReadDto cloturerExercice(String keycloakIdAppelant, String exerciceId);
}
