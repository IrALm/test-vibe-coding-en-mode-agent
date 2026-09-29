package com.walsia.api_compta.integrationClient.specification;

import com.walsia.api_compta.integrationClient.entity.referentiel.ClasseCompteComptable;
import com.walsia.api_compta.integrationClient.entity.referentiel.CompteComptable;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class ClasseCompteComptableSpecification {

    /** referentielComptableId : frontière de sécurité (isolation tenant), toujours appliqué, jamais un critère optionnel. */
    public Specification<ClasseCompteComptable> build(
            String referentielComptableId, String planComptableId, String entiteId, String q) {
        return Specification
                .where(parReferentiel(referentielComptableId))
                .and(parRecherche(planComptableId, entiteId, q));
    }

    private Specification<ClasseCompteComptable> parReferentiel(String referentielComptableId) {
        return (root, query, cb) -> cb.equal(root.get("referentielComptable").get("id"), referentielComptableId);
    }

    /**
     * Une classe matche si son propre numéro/titre correspond à q, OU si un de ses comptes
     * (numéro/libellé) correspond - pour qu'une recherche par numéro de compte (ex. "411") retrouve
     * la classe qui le contient, pas seulement une recherche par numéro de classe (ex. "4").
     */
    private Specification<ClasseCompteComptable> parRecherche(String planComptableId, String entiteId, String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return null;
            }
            // Préfixe, pas "contient" : la codification décimale SYSCOHADA fait qu'un numéro de compte
            // peut contenir n'importe quel chiffre n'importe où (ex. "704" contient "4") - seul un
            // préfixe identifie réellement la classe/sous-hiérarchie recherchée (ex. "41" -> 411, 4111).
            String prefixeNumero = q.trim().toLowerCase() + "%";
            String motifTexte = "%" + RechercheTexteUtils.normaliser(q) + "%";

            // .as(String.class) ne suffit pas ici : sur un Path<Integer>, il ne fait que
            // retyper l'expression côté Java, sans émettre de CAST SQL - Postgres refuse
            // alors "numero LIKE ?" (integer ~~ text). Il faut un vrai cast SQL explicite.
            @SuppressWarnings({"unchecked", "rawtypes"})
            JpaExpression<Integer> numero = (JpaExpression<Integer>) (JpaExpression) root.get("numero");
            Expression<String> numeroTexte = ((HibernateCriteriaBuilder) cb).cast(numero, String.class);
            Expression<String> titreNormalise = cb.function("unaccent", String.class, cb.lower(root.get("titre")));

            Predicate matchClasse = cb.or(
                    cb.like(numeroTexte, "%" + q.trim() + "%"),
                    cb.like(titreNormalise, motifTexte));

            Subquery<String> compteCorrespondant = query.subquery(String.class);
            var compteRoot = compteCorrespondant.from(CompteComptable.class);
            Expression<String> compteNumeroNormalise = cb.lower(compteRoot.get("numero"));
            Expression<String> compteLibelleNormalise =
                    cb.function("unaccent", String.class, cb.lower(compteRoot.get("libelle")));
            compteCorrespondant.select(compteRoot.get("id")).where(
                    cb.equal(compteRoot.get("classeCompteComptable"), root),
                    cb.equal(compteRoot.get("planComptable").get("id"), planComptableId),
                    cb.or(cb.isNull(compteRoot.get("entite")), cb.equal(compteRoot.get("entite").get("id"), entiteId)),
                    cb.or(cb.like(compteNumeroNormalise, prefixeNumero), cb.like(compteLibelleNormalise, motifTexte)));

            return cb.or(matchClasse, cb.exists(compteCorrespondant));
        };
    }
}
