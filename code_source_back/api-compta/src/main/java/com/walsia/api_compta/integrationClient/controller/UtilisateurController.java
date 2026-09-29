package com.walsia.api_compta.integrationClient.controller;

import com.walsia.api_compta.integrationClient.dto.formDto.UtilisateurCreationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.UtilisateurSearchForm;
import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurPageReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurReadDto;
import com.walsia.api_compta.integrationClient.service.interfaces.UtilisateurConsultationService;
import com.walsia.api_compta.integrationClient.service.interfaces.UtilisateurGestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private final UtilisateurConsultationService utilisateurConsultationService;
    private final UtilisateurGestionService utilisateurGestionService;

    public UtilisateurController(
            UtilisateurConsultationService utilisateurConsultationService,
            UtilisateurGestionService utilisateurGestionService) {
        this.utilisateurConsultationService = utilisateurConsultationService;
        this.utilisateurGestionService = utilisateurGestionService;
    }

    @GetMapping("/me")
    public ResponseEntity<UtilisateurReadDto> moi(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(utilisateurConsultationService.obtenirProfilConnecte(jwt.getSubject()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilisateurReadDto> ajouterUtilisateur(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UtilisateurCreationForm form) {
        UtilisateurReadDto reponse = utilisateurGestionService.ajouterUtilisateur(jwt.getSubject(), form);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @PostMapping("/rechercher")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilisateurPageReadDto> rechercherUtilisateurs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UtilisateurSearchForm form) {
        return ResponseEntity.ok(utilisateurGestionService.rechercherUtilisateurs(jwt.getSubject(), form));
    }

    @PatchMapping("/{id}/activer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilisateurReadDto> activer(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.ok(utilisateurGestionService.activer(jwt.getSubject(), id));
    }

    @PatchMapping("/{id}/desactiver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UtilisateurReadDto> desactiver(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.ok(utilisateurGestionService.desactiver(jwt.getSubject(), id));
    }
}
