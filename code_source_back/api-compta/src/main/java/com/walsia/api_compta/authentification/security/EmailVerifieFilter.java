package com.walsia.api_compta.authentification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Bloque les endpoints authentifiés tant que l'email de l'utilisateur n'est pas
 * vérifié dans Keycloak (claim standard OIDC "email_verified", mis à jour par
 * KeycloakAdminService#marquerEmailVerifie lors de la vérification).
 *
 * Exempte explicitement les routes publiques (mêmes patterns que SecurityConfig
 * #securityFilterChain) : contrairement à un resource server JWT classique, le
 * cookie de session (BFF) est envoyé par le navigateur sur TOUTE requête, y compris
 * vers un endpoint permitAll() - SessionCookieAuthenticationFilter peuple donc un
 * JwtAuthenticationToken même sur ces routes si un cookie valide est présent (ex. un
 * onglet resté connecté avec un compte non vérifié qui navigue vers la page publique
 * de création d'entreprise). Sans cette exemption, ce filtre bloquerait à tort des
 * appels qui ne devraient jamais dépendre de l'état de vérification de l'email.
 */
@Component
public class EmailVerifieFilter extends OncePerRequestFilter {

    private static final RequestMatcher ROUTES_PUBLIQUES = new OrRequestMatcher(List.of(
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/entites"),
            PathPatternRequestMatcher.pathPattern("/api/referentiels-comptables/**"),
            new AndRequestMatcher(
                    PathPatternRequestMatcher.pathPattern("/api/auth/**"),
                    new NegatedRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/definir-mot-de-passe"))
            ),
            PathPatternRequestMatcher.pathPattern("/v3/api-docs/**"),
            PathPatternRequestMatcher.pathPattern("/swagger-ui/**"),
            PathPatternRequestMatcher.pathPattern("/swagger-ui.html")
    ));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (ROUTES_PUBLIQUES.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            Boolean emailVerifie = jwt.getClaimAsBoolean("email_verified");
            if (emailVerifie == null || !emailVerifie) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Adresse email non vérifiée\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
