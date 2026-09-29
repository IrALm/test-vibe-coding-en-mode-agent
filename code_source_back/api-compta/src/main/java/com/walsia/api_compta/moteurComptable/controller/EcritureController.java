package com.walsia.api_compta.moteurComptable.controller;

import com.walsia.api_compta.moteurComptable.dto.formDto.ContrePassationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureCreationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureModificationForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureSearchForm;
import com.walsia.api_compta.moteurComptable.dto.formDto.RenvoyerBrouillonForm;
import com.walsia.api_compta.moteurComptable.dto.readDto.EcriturePageReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.EcritureReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.EcritureStatsReadDto;
import com.walsia.api_compta.moteurComptable.entity.ecriture.StatutEcriture;
import com.walsia.api_compta.moteurComptable.service.interfaces.EcritureService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ecritures")
public class EcritureController {

    private static final String ROLES_SAISIE = "hasRole('ADMIN') or hasRole('ADMIN_FINANCIER') or hasRole('COMPTABLE')";
    private static final String ROLES_VALIDATION = "hasRole('ADMIN') or hasRole('ADMIN_FINANCIER')";

    private final EcritureService ecritureService;

    public EcritureController(EcritureService ecritureService) {
        this.ecritureService = ecritureService;
    }

    @GetMapping
    public ResponseEntity<EcriturePageReadDto> rechercher(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<StatutEcriture> statuts,
            @RequestParam(required = false) String exerciceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        EcritureSearchForm form = new EcritureSearchForm(q, statuts, exerciceId, dateDebut, dateFin, sort, page, size);
        return ResponseEntity.ok(ecritureService.rechercherEcritures(jwt.getSubject(), form));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EcritureReadDto> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.ok(ecritureService.obtenirDetail(jwt.getSubject(), id));
    }

    @GetMapping("/stats")
    public ResponseEntity<EcritureStatsReadDto> stats(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String exerciceId) {
        return ResponseEntity.ok(ecritureService.obtenirStats(jwt.getSubject(), exerciceId));
    }

    @PostMapping
    @PreAuthorize(ROLES_SAISIE)
    public ResponseEntity<EcritureReadDto> creer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EcritureCreationForm form) {
        EcritureReadDto reponse = ecritureService.creerEcriture(jwt.getSubject(), form);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @PatchMapping("/{id}")
    @PreAuthorize(ROLES_SAISIE)
    public ResponseEntity<EcritureReadDto> modifier(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @Valid @RequestBody EcritureModificationForm form) {
        return ResponseEntity.ok(ecritureService.modifierEcriture(jwt.getSubject(), id, form));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ROLES_SAISIE)
    public ResponseEntity<Void> supprimer(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        ecritureService.supprimerEcriture(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/soumettre")
    @PreAuthorize(ROLES_SAISIE)
    public ResponseEntity<EcritureReadDto> soumettre(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.ok(ecritureService.soumettre(jwt.getSubject(), id));
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize(ROLES_VALIDATION)
    public ResponseEntity<EcritureReadDto> valider(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.ok(ecritureService.valider(jwt.getSubject(), id));
    }

    @PostMapping("/{id}/renvoyer-en-brouillon")
    @PreAuthorize(ROLES_VALIDATION)
    public ResponseEntity<EcritureReadDto> renvoyerEnBrouillon(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody(required = false) RenvoyerBrouillonForm form) {
        RenvoyerBrouillonForm corps = form != null ? form : new RenvoyerBrouillonForm(null);
        return ResponseEntity.ok(ecritureService.renvoyerEnBrouillon(jwt.getSubject(), id, corps));
    }

    @PostMapping("/{id}/contre-passer")
    @PreAuthorize(ROLES_VALIDATION)
    public ResponseEntity<EcritureReadDto> contrePasser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody(required = false) ContrePassationForm form) {
        ContrePassationForm corps = form != null ? form : new ContrePassationForm(null);
        EcritureReadDto reponse = ecritureService.contrePasser(jwt.getSubject(), id, corps);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }
}
