package com.walsia.api_compta.integrationClient.controller;

import com.walsia.api_compta.integrationClient.dto.formDto.TiersAssociationCompteForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersCreationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersModificationForm;
import com.walsia.api_compta.integrationClient.dto.formDto.TiersSearchForm;
import com.walsia.api_compta.integrationClient.dto.readDto.TiersPageReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.TiersReadDto;
import com.walsia.api_compta.integrationClient.dto.readDto.TiersRecapReadDto;
import com.walsia.api_compta.integrationClient.entity.tiers.TypeTiers;
import com.walsia.api_compta.integrationClient.service.interfaces.TiersService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/tiers")
public class TiersController {

    private final TiersService tiersService;

    public TiersController(TiersService tiersService) {
        this.tiersService = tiersService;
    }

    @GetMapping
    public ResponseEntity<TiersPageReadDto> rechercher(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TypeTiers type,
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        TiersSearchForm form = new TiersSearchForm(q, type, actif, sort, page, size);
        return ResponseEntity.ok(tiersService.rechercherTiers(jwt.getSubject(), form));
    }

    @GetMapping("/recap")
    public ResponseEntity<TiersRecapReadDto> recap(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(tiersService.obtenirRecap(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TiersReadDto> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.ok(tiersService.obtenirDetail(jwt.getSubject(), id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE')")
    public ResponseEntity<TiersReadDto> creer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TiersCreationForm form) {
        TiersReadDto reponse = tiersService.creerTiers(jwt.getSubject(), form);
        return ResponseEntity.status(HttpStatus.CREATED).body(reponse);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE')")
    public ResponseEntity<TiersReadDto> modifier(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @Valid @RequestBody TiersModificationForm form) {
        return ResponseEntity.ok(tiersService.modifierTiers(jwt.getSubject(), id, form));
    }

    @PatchMapping("/{id}/compte-associe")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE')")
    public ResponseEntity<TiersReadDto> associerCompte(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @RequestBody TiersAssociationCompteForm form) {
        return ResponseEntity.ok(tiersService.associerCompte(jwt.getSubject(), id, form));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPTABLE')")
    public ResponseEntity<TiersReadDto> desactiver(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.ok(tiersService.desactiverTiers(jwt.getSubject(), id));
    }
}
