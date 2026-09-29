package com.walsia.api_compta.authentification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Défense CSRF en profondeur (double-submit cookie), en complément de
 * SameSite=Strict sur le cookie de session. Sur toute requête à méthode non
 * sûre qui porte le cookie de session, exige un header X-XSRF-TOKEN
 * strictement identique au cookie XSRF-TOKEN de la même requête (posé au
 * login, non httpOnly). Aucune vérification côté serveur au-delà de cette
 * égalité : la sécurité vient de la same-origin policy - un site tiers ne
 * peut pas lire notre cookie pour reproduire le header attendu, même s'il
 * parvient à déclencher la requête (ex. formulaire caché, image).
 *
 * Ne s'applique jamais à un visiteur sans cookie de session (ex. POST
 * /api/entites, anonyme) ni aux méthodes sûres (GET/HEAD/OPTIONS).
 */
@Component
public class CsrfDoubleSubmitFilter extends OncePerRequestFilter {

    private static final Set<String> METHODES_SURES = Set.of(
            HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean methodeSure = METHODES_SURES.contains(request.getMethod());
        Optional<String> cookieSession = lireCookie(request, SessionCookieHelper.NOM_COOKIE);

        if (!methodeSure && cookieSession.isPresent()) {
            Optional<String> cookieCsrf = lireCookie(request, SessionCookieHelper.NOM_COOKIE_CSRF);
            String headerCsrf = request.getHeader(SessionCookieHelper.NOM_HEADER_CSRF);

            if (cookieCsrf.isEmpty() || headerCsrf == null || !cookieCsrf.get().equals(headerCsrf)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Jeton CSRF invalide ou absent\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> lireCookie(HttpServletRequest request, String nom) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> nom.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
