package com.walsia.api_compta.integrationClient.specification;

import com.walsia.api_compta.integrationClient.dto.formDto.UtilisateurSearchForm;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Role;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class UtilisateurSpecification {

    /** entiteId : frontière de sécurité (isolation tenant), toujours appliqué, jamais un critère optionnel. */
    public Specification<Utilisateur> build(UtilisateurSearchForm form, String entiteId) {
        return Specification
                .where(parEntite(entiteId))
                .and(parNom(form.nom()))
                .and(parEmail(form.email()))
                .and(parRole(form.role()))
                .and(parActif(form.actif()));
    }

    private Specification<Utilisateur> parEntite(String entiteId) {
        return (root, query, cb) -> cb.equal(root.get("entite").get("id"), entiteId);
    }

    private Specification<Utilisateur> parNom(String nom) {
        return (root, query, cb) -> {
            if (nom == null || nom.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("nom")), "%" + nom.toLowerCase() + "%");
        };
    }

    private Specification<Utilisateur> parEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isBlank()) {
                return null;
            }
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
        };
    }

    private Specification<Utilisateur> parRole(Role role) {
        return (root, query, cb) -> {
            if (role == null) {
                return null;
            }
            return cb.equal(root.get("role"), role);
        };
    }

    private Specification<Utilisateur> parActif(Boolean actif) {
        return (root, query, cb) -> {
            if (actif == null) {
                return null;
            }
            return cb.equal(root.get("actif"), actif);
        };
    }
}
