# Services Docker Compose

## Démarrer les services

```bash
docker compose up -d
```

## Accès aux services

### Keycloak
- URL : http://localhost:8081
- Identifiants admin : `${KC_ADMIN_USERNAME}` / `${KC_ADMIN_PASSWORD}` (définis dans `.env`)

### pgAdmin
- URL : http://localhost:5050
- Identifiants : `${PGADMIN_DEFAULT_EMAIL}` / `${PGADMIN_DEFAULT_PASSWORD}` (définis dans `.env`)

### PostgreSQL - Metier
- Host : `localhost`
- Port : `5433`
- Base : `${POSTGRES_METIER_DB}`
- Utilisateur / mot de passe : `${POSTGRES_METIER_USER}` / `${POSTGRES_METIER_PASSWORD}`

```bash
psql -h localhost -p 5433 -U ${POSTGRES_METIER_USER} -d ${POSTGRES_METIER_DB}
```

### PostgreSQL - Keycloak
> Non exposé sur l'hôte (accessible uniquement depuis le réseau `keycloak-network`, ex. via pgAdmin).

```bash
docker exec -it postgres-keycloak psql -U ${POSTGRES_KEYCLOAK_USER} -d ${POSTGRES_KEYCLOAK_DB}
```

## Arrêter les services

```bash
docker compose down
```
