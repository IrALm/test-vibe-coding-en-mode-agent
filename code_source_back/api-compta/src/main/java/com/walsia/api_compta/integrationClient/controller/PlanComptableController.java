package com.walsia.api_compta.integrationClient.controller;

import com.walsia.api_compta.integrationClient.dto.readDto.PlanComptableRecapReadDto;
import com.walsia.api_compta.integrationClient.service.interfaces.PlanComptableConsultationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plan-comptable")
public class PlanComptableController {

    private final PlanComptableConsultationService planComptableConsultationService;

    public PlanComptableController(PlanComptableConsultationService planComptableConsultationService) {
        this.planComptableConsultationService = planComptableConsultationService;
    }

    @GetMapping("/recap")
    public ResponseEntity<PlanComptableRecapReadDto> recap(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(planComptableConsultationService.obtenirRecap(jwt.getSubject()));
    }
}
