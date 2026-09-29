package com.walsia.compta.security;

import com.nimbusds.jwt.JWTParser;
import com.walsia.compta.tenant.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

/**
 * Résout dynamiquement l'{@link AuthenticationManager} à utiliser en
 * fonction de l'issuer (claim {@code iss}, non vérifié à ce stade) porté par
 * le jeton JWT de la requête.
 * <p>
 * L'issuer doit correspondre soit au realm admin configuré
 * ({@code compta.keycloak.admin-realm}), soit à un realm tenant connu de
 * l'annuaire ({@link TenantRepository#findByKeycloakRealm(String)}) ; sinon
 * la requête est rejetée (401).
 * <p>
 * <b>Important (sécurité) :</b> le nom de realm extrait du claim {@code iss}
 * n'est PAS fiable à ce stade (le jeton n'est pas encore vérifié). Il ne sert
 * donc qu'à identifier un realm <em>candidat</em>. L'issuer effectivement
 * utilisé pour résoudre la configuration OIDC/JWKS (et donc déclencher un
 * appel réseau via {@link JwtDecoders#fromIssuerLocation(String)}) est
 * TOUJOURS reconstruit côté serveur à partir de la base URL Keycloak de
 * confiance ({@code compta.keycloak.base-url}) et du nom de realm candidat —
 * jamais à partir de la valeur brute fournie par le token. L'issuer du token
 * doit correspondre EXACTEMENT (égalité stricte de chaîne) à cet issuer
 * reconstruit, faute de quoi la requête est rejetée avant tout appel réseau.
 * Ce principe est le même que celui appliqué dans
 * {@link AdminAccessAuthorizationManager} pour le realm admin ; il est ici
 * étendu aux realms tenant afin d'empêcher un attaquant de forger un jeton
 * avec {@code iss = https://attacker.example/realms/<realm-connu>} pour
 * provoquer une résolution SSRF vers un hôte arbitraire et usurper un
 * contexte tenant complet.
 * <p>
 * Un {@link AuthenticationManager} (et le {@link JwtDecoder} associé) est
 * construit une seule fois par issuer de confiance via
 * {@link JwtDecoders#fromIssuerLocation(String)}, puis mis en cache : aucun
 * appel réseau supplémentaire n'est effectué sur les requêtes suivantes pour
 * un même issuer.
 */
@Slf4j
@Component
public class TenantAwareAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    private final TenantRepository tenantRepository;
    private final String adminRealm;
    private final String keycloakBaseUrl;
    private final BearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();
    private final Map<String, AuthenticationManager> managersByIssuer = new ConcurrentHashMap<>();

    public TenantAwareAuthenticationManagerResolver(
            TenantRepository tenantRepository,
            @Value("${compta.keycloak.base-url}") String keycloakBaseUrl,
            @Value("${compta.keycloak.admin-realm}") String adminRealm) {
        this.tenantRepository = tenantRepository;
        this.keycloakBaseUrl = keycloakBaseUrl.replaceAll("/+$", "");
        this.adminRealm = adminRealm;
    }

    @Override
    public AuthenticationManager resolve(HttpServletRequest request) {
        String tokenIssuer = extractUnverifiedIssuer(request);
        if (tokenIssuer == null) {
            throw new InvalidBearerTokenException("Jeton JWT invalide ou absent (claim 'iss' introuvable)");
        }

        // Le nom de realm extrait ici n'est qu'un CANDIDAT non fiable : il ne
        // sert qu'à retrouver le realm attendu, jamais à construire l'issuer
        // effectivement utilisé pour la résolution réseau.
        String candidateRealm = KeycloakIssuerSupport.extractRealm(tokenIssuer);
        boolean isAdminRealm = adminRealm.equals(candidateRealm);
        boolean isKnownTenantRealm = !isAdminRealm && candidateRealm != null
                && tenantRepository.findByKeycloakRealm(candidateRealm).isPresent();

        if (!isAdminRealm && !isKnownTenantRealm) {
            log.warn("Jeton rejeté : realm inconnu (issuer prétendu '{}')", tokenIssuer);
            throw new InvalidBearerTokenException("Issuer inconnu : " + tokenIssuer);
        }

        // Issuer de CONFIANCE, reconstruit uniquement à partir de la base URL
        // Keycloak configurée et du nom de realm candidat — jamais à partir
        // de la valeur brute du token. Toute divergence avec l'issuer prétendu
        // par le token (host différent, protocole différent, etc.) est
        // rejetée avant tout appel réseau.
        String trustedIssuer = keycloakBaseUrl + "/realms/" + candidateRealm;
        if (!trustedIssuer.equals(tokenIssuer)) {
            log.warn("Jeton rejeté : issuer '{}' ne correspond pas à l'issuer de confiance attendu '{}'",
                    tokenIssuer, trustedIssuer);
            throw new InvalidBearerTokenException("Issuer non autorisé : " + tokenIssuer);
        }

        return managersByIssuer.computeIfAbsent(trustedIssuer, this::buildAuthenticationManager);
    }

    private AuthenticationManager buildAuthenticationManager(String issuer) {
        log.info("Construction (mise en cache) de l'AuthenticationManager pour l'issuer '{}'", issuer);
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleGrantedAuthoritiesConverter());

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(authenticationConverter);
        return provider::authenticate;
    }

    private String extractUnverifiedIssuer(HttpServletRequest request) {
        String token;
        try {
            token = bearerTokenResolver.resolve(request);
        } catch (RuntimeException e) {
            return null;
        }
        if (token == null) {
            return null;
        }
        try {
            Object issuer = JWTParser.parse(token).getJWTClaimsSet().getIssuer();
            return issuer != null ? issuer.toString() : null;
        } catch (ParseException e) {
            return null;
        }
    }
}
