-- Seed des référentiels comptables disponibles à la création d'une entité.
-- Les comptes officiels (classes + comptes) de chaque référentiel sont hors
-- scope de cette migration : ils seront alimentés séparément, et remonteront
-- automatiquement une fois les tables classe_compte_comptable / compte_comptable
-- renseignées (aucun changement de code nécessaire côté application).


INSERT INTO referentiel_comptable (id, code, libelle, description, version)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'SYSCOHADA_NORMAL', 'SYSCOHADA - Système Normal',
     'Système normal : entreprises importantes, bilan + compte de résultat + TFT + annexes complètes', 'Révisé 2017'),
    ('00000000-0000-0000-0000-000000000002', 'SYSCOHADA_SMT', 'SYSCOHADA - Système Minimal de Trésorerie',
     'Système minimal de trésorerie : très petites entreprises', 'Révisé 2017'),
    ('00000000-0000-0000-0000-000000000003', 'SYCEBNL', 'SYCEBNL',
     'Référentiel dédié aux associations, ONG et entités à but non lucratif', '1ère édition');
