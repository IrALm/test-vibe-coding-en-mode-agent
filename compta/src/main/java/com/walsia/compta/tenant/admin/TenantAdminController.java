package com.walsia.compta.tenant.admin;

import com.walsia.compta.tenant.admin.dto.TenantCreateRequest;
import com.walsia.compta.tenant.admin.dto.TenantResponse;
import com.walsia.compta.tenant.admin.dto.TenantUpdateRequest;
import com.walsia.compta.tenant.domain.Tenant;
import com.walsia.compta.tenant.repository.TenantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API d'administration de l'annuaire des tenants. Protégée par
 * {@code hasRole(SUPER_ADMIN)} + vérification stricte de l'issuer (realm
 * admin), voir {@code SecurityConfig} / {@code AdminAccessAuthorizationManager}.
 */
@RestController
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
public class TenantAdminController {

    private final TenantProvisioningService provisioningService;
    private final TenantRepository tenantRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@RequestBody TenantCreateRequest request) {
        return provisioningService.create(request);
    }

    @GetMapping
    public List<TenantResponse> list() {
        return tenantRepository.findAll().stream().map(TenantResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TenantResponse getById(@PathVariable Long id) {
        Tenant tenant = tenantRepository.findById(id).orElseThrow(() -> new TenantNotFoundException(id));
        return TenantResponse.from(tenant);
    }

    @PatchMapping("/{id}")
    public TenantResponse update(@PathVariable Long id, @RequestBody TenantUpdateRequest request) {
        return provisioningService.update(id, request);
    }

    @PostMapping("/{id}/retry-migration")
    public TenantResponse retryMigration(@PathVariable Long id) {
        return provisioningService.retryMigration(id);
    }
}
