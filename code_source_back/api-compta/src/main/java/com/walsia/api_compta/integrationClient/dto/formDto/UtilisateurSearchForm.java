package com.walsia.api_compta.integrationClient.dto.formDto;

import com.walsia.api_compta.integrationClient.entity.utilisateur.Role;
import com.walsia.api_compta.integrationClient.entity.utilisateur.UtilisateurSortEnum;
import org.springframework.data.domain.Sort;

public record UtilisateurSearchForm(
        String nom,
        String email,
        Role role,
        Boolean actif,
        Integer page,
        Integer size,
        String sortBy,
        Sort.Direction sortDirection
) {
    public UtilisateurSearchForm {
        page = page != null ? page : 0;
        size = size != null ? size : 10;
        sortBy = UtilisateurSortEnum.resolveField(sortBy);
        sortDirection = sortDirection != null ? sortDirection : Sort.Direction.DESC;
    }
}
