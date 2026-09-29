package com.walsia.api_compta.integrationClient.controller;

import com.walsia.api_compta.integrationClient.dto.readDto.ReferentielComptableReadDto;
import com.walsia.api_compta.integrationClient.mapper.ReferentielComptableMapper;
import com.walsia.api_compta.integrationClient.repository.ReferentielComptableRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/referentiels-comptables")
public class ReferentielComptableController {

    private final ReferentielComptableRepository referentielComptableRepository;
    private final ReferentielComptableMapper referentielComptableMapper;

    public ReferentielComptableController(
            ReferentielComptableRepository referentielComptableRepository,
            ReferentielComptableMapper referentielComptableMapper) {
        this.referentielComptableRepository = referentielComptableRepository;
        this.referentielComptableMapper = referentielComptableMapper;
    }

    @GetMapping
    public List<ReferentielComptableReadDto> lister() {
        return referentielComptableRepository.findAll().stream()
                .map(referentielComptableMapper::toReadDto)
                .toList();
    }
}
