package com.walsia.api_compta.moteurComptable.specification;

import com.walsia.api_compta.integrationClient.specification.RechercheTexteUtils;
import com.walsia.api_compta.moteurComptable.dto.formDto.EcritureSearchForm;
import com.walsia.api_compta.moteurComptable.entity.ecriture.Ecriture;
import com.walsia.api_compta.moteurComptable.entity.ecriture.StatutEcriture;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class EcritureSpecification {

    /** entiteId : frontière de sécurité (isolation tenant), toujours appliqué, jamais un critère optionnel. */
    public Specification<Ecriture> build(String entiteId, EcritureSearchForm form) {
        return Specification
                .where(parEntite(entiteId))
                .and(parRecherche(form.q()))
                .and(parStatuts(form.statuts()))
                .and(parExercice(form.exerciceId()))
                .and(parPeriode(form.dateDebut(), form.dateFin()));
    }

    private Specification<Ecriture> parEntite(String entiteId) {
        return (root, query, cb) -> cb.equal(root.get("entite").get("id"), entiteId);
    }

    private Specification<Ecriture> parRecherche(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return null;
            String motifNormalise = "%" + RechercheTexteUtils.normaliser(q) + "%";
            Expression<String> libelleNormalise = cb.function("unaccent", String.class, cb.lower(root.get("libelle")));
            Expression<String> referenceNormalisee = cb.function("unaccent", String.class, cb.lower(root.get("reference")));
            return cb.or(
                    cb.like(libelleNormalise, motifNormalise),
                    cb.like(referenceNormalisee, motifNormalise),
                    cb.like(cb.lower(root.get("numero")), "%" + q.trim().toLowerCase() + "%"));
        };
    }

    private Specification<Ecriture> parStatuts(List<StatutEcriture> statuts) {
        return (root, query, cb) -> (statuts == null || statuts.isEmpty()) ? null : root.get("statut").in(statuts);
    }

    private Specification<Ecriture> parExercice(String exerciceId) {
        return (root, query, cb) -> exerciceId == null ? null : cb.equal(root.get("exercice").get("id"), exerciceId);
    }

    private Specification<Ecriture> parPeriode(LocalDate debut, LocalDate fin) {
        return (root, query, cb) -> {
            if (debut == null && fin == null) return null;
            if (debut != null && fin != null) return cb.between(root.get("date"), debut, fin);
            return debut != null
                    ? cb.greaterThanOrEqualTo(root.get("date"), debut)
                    : cb.lessThanOrEqualTo(root.get("date"), fin);
        };
    }
}
