-- Marque l'écriture de clôture auto-générée (solde des comptes de gestion 6/7/8 vers le
-- compte de résultat 111/119) pour l'exclure du calcul du résultat de CET exercice (sinon
-- l'écriture de clôture s'annulerait elle-même dans calculerResultat dès sa création).
ALTER TABLE ecriture
    ADD COLUMN genere_par_cloture BOOLEAN NOT NULL DEFAULT FALSE;
