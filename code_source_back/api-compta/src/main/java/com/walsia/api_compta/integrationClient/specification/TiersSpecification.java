package com.walsia.api_compta.integrationClient.specification;

import com.walsia.api_compta.integrationClient.dto.formDto.TiersSearchForm;
import com.walsia.api_compta.integrationClient.entity.tiers.Tiers;
import com.walsia.api_compta.integrationClient.entity.tiers.TypeTiers;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class TiersSpecification {

    /** entiteId : frontière de sécurité (isolation tenant), toujours appliqué, jamais un critère optionnel. */
    public Specification<Tiers> build(String entiteId, TiersSearchForm form) {
        return Specification
                .where(parEntite(entiteId))
                .and(parRecherche(form.q()))
                .and(parType(form.type()))
                .and(parActif(form.actif()));
    }

    private Specification<Tiers> parEntite(String entiteId) {
        return (root, query, cb) -> cb.equal(root.get("entite").get("id"), entiteId);
    }

    private Specification<Tiers> parRecherche(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return null;
            }
            String motifNormalise = "%" + RechercheTexteUtils.normaliser(q) + "%";
            String motifBrut = "%" + q.trim().toLowerCase() + "%";
            Expression<String> raisonSocialeNormalisee =
                    cb.function("unaccent", String.class, cb.lower(root.get("raisonSociale")));
            Expression<String> nomContactNormalise =
                    cb.function("unaccent", String.class, cb.lower(root.get("nomContact")));
            return cb.or(
                    cb.like(raisonSocialeNormalisee, motifNormalise),
                    cb.like(nomContactNormalise, motifNormalise),
                    cb.like(cb.lower(root.get("email")), motifBrut),
                    cb.like(cb.lower(root.get("numeroFiscal")), motifBrut));
        };
    }

    private Specification<Tiers> parType(TypeTiers type) {
        return (root, query, cb) -> {
            if (type == null) {
                return null;
            }
            return cb.equal(root.get("type"), type);
        };
    }

    private Specification<Tiers> parActif(Boolean actif) {
        return (root, query, cb) -> {
            if (actif == null) {
                return null;
            }
            return cb.equal(root.get("actif"), actif);
        };
    }
}
