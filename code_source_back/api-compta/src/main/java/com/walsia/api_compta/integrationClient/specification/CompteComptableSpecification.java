package com.walsia.api_compta.integrationClient.specification;

import com.walsia.api_compta.integrationClient.dto.formDto.CompteComptableSearchForm;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.SensCompte;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class CompteComptableSpecification {

    /**
     * planComptableId/entiteId : frontière de sécurité (isolation tenant + scope plan actif), toujours
     * appliqués, jamais des critères optionnels. classeId reste une frontière de sécurité quand fourni
     * (recherche scopée à une classe), mais devient null pour la recherche globale (toutes classes) -
     * dans ce cas ce n'est plus une frontière de sécurité, juste un filtre optionnel absent.
     */
    public Specification<CompteComptable> build(
            String classeId, String planComptableId, String entiteId, CompteComptableSearchForm form) {
        return Specification
                .where(parPlanComptable(planComptableId))
                .and(parClasse(classeId))
                .and(parPortee(entiteId))
                .and(parRecherche(form.q()))
                .and(parSens(form.sens()));
    }

    /**
     * Ni .where(...) ni .and(...) ne tolèrent qu'on leur passe null en tant que Specification (l'objet
     * lambda lui-même) - contrairement au Predicate qu'une Specification peut renvoyer une fois
     * invoquée, qui lui peut être null (= pas de restriction), comme parRecherche/parSens ci-dessous.
     * Donc toujours retourner une Specification non-null ici, et ne renvoyer null qu'à l'intérieur.
     */
    private Specification<CompteComptable> parClasse(String classeId) {
        return (root, query, cb) -> classeId == null
                ? null
                : cb.equal(root.get("classeCompteComptable").get("id"), classeId);
    }

    private Specification<CompteComptable> parPlanComptable(String planComptableId) {
        return (root, query, cb) -> cb.equal(root.get("planComptable").get("id"), planComptableId);
    }

    /** Comptes standards (entite = null) et sous-comptes spécifiques de l'entreprise appelante : jamais ceux d'une autre entreprise. */
    private Specification<CompteComptable> parPortee(String entiteId) {
        return (root, query, cb) -> cb.or(
                cb.isNull(root.get("entite")),
                cb.equal(root.get("entite").get("id"), entiteId));
    }

    private Specification<CompteComptable> parRecherche(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return null;
            }
            Expression<String> numeroNormalise = cb.lower(root.get("numero"));
            Expression<String> libelleNormalise = cb.function("unaccent", String.class, cb.lower(root.get("libelle")));
            String motif = "%" + RechercheTexteUtils.normaliser(q) + "%";
            return cb.or(
                    cb.like(numeroNormalise, "%" + q.trim().toLowerCase() + "%"),
                    cb.like(libelleNormalise, motif));
        };
    }

    private Specification<CompteComptable> parSens(SensCompte sens) {
        return (root, query, cb) -> {
            if (sens == null) {
                return null;
            }
            return cb.equal(root.get("sensNormal"), sens);
        };
    }
}
