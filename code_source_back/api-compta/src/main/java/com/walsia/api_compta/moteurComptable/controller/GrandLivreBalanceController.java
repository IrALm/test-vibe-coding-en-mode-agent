package com.walsia.api_compta.moteurComptable.controller;

import com.walsia.api_compta.moteurComptable.dto.readDto.BalanceReadDto;
import com.walsia.api_compta.moteurComptable.dto.readDto.GrandLivreReadDto;
import com.walsia.api_compta.moteurComptable.service.interfaces.GrandLivreBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comptabilite")
public class GrandLivreBalanceController {

    private final GrandLivreBalanceService grandLivreBalanceService;

    public GrandLivreBalanceController(GrandLivreBalanceService grandLivreBalanceService) {
        this.grandLivreBalanceService = grandLivreBalanceService;
    }

    @GetMapping("/grand-livre")
    public ResponseEntity<GrandLivreReadDto> grandLivre(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String exerciceId,
            @RequestParam String compteId) {
        return ResponseEntity.ok(grandLivreBalanceService.obtenirGrandLivre(jwt.getSubject(), exerciceId, compteId));
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceReadDto> balance(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String exerciceId) {
        return ResponseEntity.ok(grandLivreBalanceService.obtenirBalance(jwt.getSubject(), exerciceId));
    }
}
