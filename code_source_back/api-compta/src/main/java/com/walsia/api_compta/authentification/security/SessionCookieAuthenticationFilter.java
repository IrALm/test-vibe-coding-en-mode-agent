package com.walsia.api_compta.authentification.security;

import com.walsia.api_compta.exception.AuthentificationEchoueeException;
import com.walsia.api_compta.authentification.service.interfaces.AuthSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Authentifie via le cookie de session opaque (BFF) quand aucun Bearer JWT n'a
 * déjà authentifié la requête. Résout la session (avec refresh transparent si
 * besoin) puis décode le vrai JWT Keycloak obtenu via AuthSessionService avec
 * le même JwtDecoder/JwtAuthenticationConverter que le flux Bearer classique,
 * pour que EmailVerifieFilter et le mapping de rôles restent inchangés.
 */
@Component
public class SessionCookieAuthenticationFilter extends OncePerRequestFilter {

    private final SessionCookieHelper sessionCookieHelper;
    private final AuthSessionService authSessionService;
    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public SessionCookieAuthenticationFilter(
            SessionCookieHelper sessionCookieHelper,
            AuthSessionService authSessionService,
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.sessionCookieHelper = sessionCookieHelper;
        this.authSessionService = authSessionService;
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<String> tokenSession = sessionCookieHelper.lireToken(request);
            if (tokenSession.isPresent()) {
                try {
                    String accessTokenBrut = authSessionService.resoudreSession(tokenSession.get());
                    Jwt jwt = jwtDecoder.decode(accessTokenBrut);
                    JwtAuthenticationToken authentication = (JwtAuthenticationToken) jwtAuthenticationConverter.convert(jwt);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (AuthentificationEchoueeException | JwtException e) {
                    // Session invalide/expirée : on laisse la requête non authentifiée,
                    // les endpoints protégés répondront 401 via le point d'entrée standard.
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
