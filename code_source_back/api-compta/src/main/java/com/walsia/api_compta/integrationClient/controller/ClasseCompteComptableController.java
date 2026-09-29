package com.walsia.api_compta.integrationClient.controller;

import com.walsia.api_compta.integrationClient.dto.formDto.CompteComptableSearchForm;
import com.walsia.api_compta.integrationClient.dto.readDto.ClasseCompteComptableReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.CompteComptablePageReadDto;
import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;
import com.walsia.api_compta.integrationClient.service.interfaces.ClasseCompteComptableService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/classes-comptables")
public class ClasseCompteComptableController {

    private final ClasseCompteComptableService classeCompteComptableService;

    public ClasseCompteComptableController(ClasseCompteComptableService classeCompteComptableService) {
        this.classeCompteComptableService = classeCompteComptableService;
    }

    @GetMapping
    public ResponseEntity<List<ClasseCompteComptableReadDto>> listerClasses(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(classeCompteComptableService.listerClasses(jwt.getSubject(), q));
    }

    @GetMapping("/{id}/comptes")
    public ResponseEntity<CompteComptablePageReadDto> rechercherComptes(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SensCompte sens,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        CompteComptableSearchForm form = new CompteComptableSearchForm(q, sens, page, size);
        return ResponseEntity.ok(classeCompteComptableService.rechercherComptes(jwt.getSubject(), id, form));
    }
}
