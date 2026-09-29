# Authentification — état actuel

## Principe

Architecture **backend-mediated (BFF)**. Le frontend échange email/mot de passe contre
un **cookie opaque httpOnly** — jamais un JWT Keycloak. Le backend détient et rafraîchit
les tokens Keycloak (access + refresh) pour le compte du frontend, chiffrés en base.

## Ce que ça résout

- **Vol de token via XSS** : impossible — le JS ne voit jamais de JWT ni de refresh token.
- **Déconnexion brutale pendant l'usage actif** : rafraîchissement transparent côté serveur
  + heartbeat frontend (ping léger tant que l'onglet est visible).
- **CSRF** : `SameSite=Strict` + cookie CSRF double-submit.
- **Brute-force / credential stuffing** : lockout Keycloak (`bruteForceProtected`) + rate
  limiting applicatif par IP sur les endpoints sensibles.
- **Fuite de données inter-entreprise** : isolation tenant imposée côté serveur (jamais un
  filtre fourni par le client).
- **Session figée en base de données claire** : tokens Keycloak stockés chiffrés (AES-256-GCM),
  jamais en clair.

## Flow

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {
  'primaryColor': '#eef6ff',
  'primaryBorderColor': '#2563eb',
  'primaryTextColor': '#1e293b',
  'actorBkg': '#eef6ff',
  'actorBorder': '#2563eb',
  'signalColor': '#334155',
  'signalTextColor': '#1e293b',
  'noteBkgColor': '#fef9c3',
  'noteBorderColor': '#ca8a04'
}}}%%
sequenceDiagram
    actor U as Navigateur
    participant F as Frontend Angular
    participant B as api-compta (backend)
    participant K as Keycloak

    rect rgb(220, 245, 225)
    Note over U,K: Login
    U->>F: email + mot de passe
    F->>B: POST /api/auth/login
    B->>K: grant_type=password (ROPC)
    K-->>B: access_token + refresh_token
    B->>B: chiffrer (AES-256-GCM) + stocker dans UserSession (Postgres)
    B->>B: générer un token opaque, hash stocké
    B-->>F: 200 + Set-Cookie SESSION_ID (httpOnly) + XSRF-TOKEN
    end

    rect rgb(222, 235, 255)
    Note over U,K: Requête authentifiée
    F->>B: requête (cookie SESSION_ID + header X-XSRF-TOKEN)
    B->>B: hash du cookie → retrouve UserSession (verrou pessimiste)
    alt access token encore valide
        B->>B: déchiffre, utilise directement
    else access token expiré, refresh encore valide
        B->>K: grant_type=refresh_token
        K-->>B: nouveau access_token + refresh_token
        B->>B: re-chiffre, met à jour UserSession
    else refresh token expiré
        B->>B: UserSession.revokedAt = now
        B-->>F: 401
    end
    B-->>F: réponse (cookies inchangés)
    end

    rect rgb(255, 228, 225)
    Note over U,K: Logout
    F->>B: POST /api/auth/logout (cookie)
    B->>K: révoque le refresh token
    B->>B: UserSession.revokedAt = now
    B-->>F: 204 + cookies effacés
    end
```

## Composants clés

| Fichier | Rôle |
|---|---|
| `AuthController` | endpoints `login` / `logout` / `definir-mot-de-passe` |
| `AuthSessionServiceImpl` | connecter / résoudre session (+ refresh) / déconnecter |
| `UserSession` (entity + migration) | tokens chiffrés, expirations, révocation |
| `TokenCipherService` | chiffrement AES-256-GCM des tokens stockés |
| `SessionCookieAuthenticationFilter` | résout le cookie à chaque requête, peuple l'authentification |
| `EmailVerifieFilter` | bloque tant que l'email n'est pas vérifié |
| `CsrfDoubleSubmitFilter` | vérifie `X-XSRF-TOKEN` == cookie `XSRF-TOKEN` |
| `RateLimitFilter` | limite les requêtes par IP sur les endpoints sensibles |
| `SessionCookieHelper` | construit/lit les cookies (session + CSRF) |
| `KeycloakAuthService` | échange identifiants/refresh contre des tokens Keycloak |

## Pourquoi PostgreSQL plutôt que Redis pour `UserSession`

- **Lecture** : indexée sur un hash unique, à chaque requête — le working set (sessions
  actives) est petit et tient dans le cache mémoire de Postgres, même à volume élevé.
- **Écriture** : seulement au rafraîchissement (~1×/5 min par utilisateur actif) — charge
  négligeable, pas de contention.
- **Cohérence** : même pattern déjà utilisé pour `UserToken` (vérification email, reset
  mot de passe) — pas de nouvelle techno à opérer/monitorer pour ce stade du projet.
- **Redis n'apporterait un gain réel qu'à un volume de requêtes** bien supérieur à celui de
  l'application actuelle — pas justifié sans données de charge réelles (optimisation
  prématurée).
- **Migration non bloquée** : la logique de session est encapsulée derrière
  `AuthSessionService`/`UserSessionRepository` — remplaçable par une implémentation Redis
  plus tard sans réécrire le reste, si le besoin apparaît.
