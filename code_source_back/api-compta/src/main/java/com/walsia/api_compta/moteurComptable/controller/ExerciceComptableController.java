package com.walsia.api_compta.moteurComptable.controller;

import com.walsia.api_compta.moteurComptable.dto.formDto.ExerciceCreationForm;
import com.walsia.api_compta.moteurComptable.dto.readDto.ClotureCheckReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.ExerciceReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.ResultatReadDto;
import com.walsia.api_compta.moteurComptable.service.interfaces.ExerciceComptableService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exercices")
public class ExerciceComptableController {

    private final ExerciceComptableService exerciceComptableService;

    public ExerciceComptableController(ExerciceComptableService exerciceComptableService) {
        this.exerciceComptableService = exerciceComptableService;
    }

    @GetMapping
    public ResponseEntity<List<ExerciceReadDto>> lister(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(exerciceComptableService.listerExercices(jwt.getSubject()));
    }

    @GetMapping("/ouvert")
    public ResponseEntity<ExerciceReadDto> ouvert(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(exerciceComptableService.obtenirExerciceOuvert(jwt.getSubject()));
    }

    @GetMapping("/{id}/resultat")
    public ResponseEntity<ResultatReadDto> resultat(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.ok(exerciceComptableService.obtenirResultat(jwt.getSubject(), id));
    }

    @GetMapping("/{id}/cloture-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClotureCheckReadDto> clotureCheck(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.ok(exerciceComptableService.verifierCloture(jwt.getSubject(), id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExerciceReadDto> creer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ExerciceCreationForm form) {
        ExerciceReadDto reponse = exerciceComptableService.creerExercice(jwt.getSubject(), form);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @PostMapping("/{id}/cloturer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExerciceReadDto> cloturer(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.ok(exerciceComptableService.cloturerExercice(jwt.getSubject(), id));
    }
}
