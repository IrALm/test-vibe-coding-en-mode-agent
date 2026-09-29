package com.walsia.api_compta.integrationClient.controller;

import com.walsia.api_compta.integrationClient.dto.formDto.EntiteCreationForm;
import com.walsia.api_compta.integrationClient.dto.readDto.EntiteCreeeReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.EntiteReadDto;
import com.walsia.api_compta.integrationClient.service.interfaces.EntiteConsultationService;
import com.walsia.api_compta.integrationClient.service.interfaces.EntiteCreationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entites")
public class EntiteController {

    private final EntiteCreationService entiteCreationService;
    private final EntiteConsultationService entiteConsultationService;

    public EntiteController(
            EntiteCreationService entiteCreationService,
            EntiteConsultationService entiteConsultationService) {
        this.entiteCreationService = entiteCreationService;
        this.entiteConsultationService = entiteConsultationService;
    }

    @PostMapping
    public ResponseEntity<EntiteCreeeReadDto> creerEntite(@Valid @RequestBody EntiteCreationForm form) {
        EntiteCreeeReadDto reponse = entiteCreationService.creerEntiteEtAdmin(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @GetMapping("/moi")
    public ResponseEntity<EntiteReadDto> monEntite(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(entiteConsultationService.obtenirEntiteConnectee(jwt.getSubject()));
    }
}
