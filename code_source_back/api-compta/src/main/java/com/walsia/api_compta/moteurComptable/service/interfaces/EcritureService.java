package com.walsia.api_compta.moteurComptable.service.interfaces;

import com.walsia.api_compta.moteurComptable.dto.formDto.ContrePassationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureCreationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureModificationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureSearchForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.RenvoyerBrouillonForm;
import com.walsia.api_compta.moteurComptable.dto.readDto.EcriturePageReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.EcritureReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.EcritureStatsReadDto;

public interface EcritureService {

    EcriturePageReadDto rechercherEcritures(String keycloakIdAppelant, EcritureSearchForm form);

    EcritureStatsReadDto obtenirStats(String keycloakIdAppelant, String exerciceId);

    EcritureReadDto obtenirDetail(String keycloakIdAppelant, String ecritureId);

    EcritureReadDto creerEcriture(String keycloakIdAppelant, EcritureCreationForm form);

    EcritureReadDto modifierEcriture(String keycloakIdAppelant, String ecritureId, EcritureModificationForm form);

    void supprimerEcriture(String keycloakIdAppelant, String ecritureId);

    EcritureReadDto soumettre(String keycloakIdAppelant, String ecritureId);

    EcritureReadDto valider(String keycloakIdAppelant, String ecritureId);

    EcritureReadDto renvoyerEnBrouillon(String keycloakIdAppelant, String ecritureId, RenvoyerBrouillonForm form);

    EcritureReadDto contrePasser(String keycloakIdAppelant, String ecritureId, ContrePassationForm form);
}
