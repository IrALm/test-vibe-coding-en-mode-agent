package com.walsia.api_compta.authentification.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Cookie de session opaque (BFF) : jamais de JWT côté navigateur. httpOnly +
 * SameSite=Strict + sans Max-Age (cookie de session, invalidé à la fermeture
 * du navigateur - l'autorité réelle reste UserSession.refreshTokenExpiresAt
 * côté serveur). Jamais d'attribut Domain (cookie host-only).
 *
 * Porte aussi le cookie CSRF (double-submit) : contrairement au cookie de
 * session, celui-ci n'est volontairement PAS httpOnly - le frontend doit
 * pouvoir le relire pour le renvoyer en header (cf. CsrfDoubleSubmitFilter).
 */
@Component
public class SessionCookieHelper {

    public static final String NOM_COOKIE = "SESSION_ID";
    public static final String NOM_COOKIE_CSRF = "XSRF-TOKEN";
    public static final String NOM_HEADER_CSRF = "X-XSRF-TOKEN";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final boolean cookieSecure;

    public SessionCookieHelper(@Value("${app.session.cookie-secure}") boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public ResponseCookie construireCookieConnexion(String tokenSessionEnClair) {
        return ResponseCookie.from(NOM_COOKIE, tokenSessionEnClair)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api")
                .build();
    }

    public ResponseCookie construireCookieDeconnexion() {
        return ResponseCookie.from(NOM_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api")
                .maxAge(0)
                .build();
    }

    public String genererValeurCsrf() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public ResponseCookie construireCookieCsrf(String valeur) {
        // Path=/ (pas /api) : ce cookie doit être lisible par document.cookie sur les
        // pages du frontend Angular (/, /admin/utilisateurs...), qui ne sont jamais
        // sous /api - contrairement au cookie de session, jamais lu en JS.
        return ResponseCookie.from(NOM_COOKIE_CSRF, valeur)
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .build();
    }

    public ResponseCookie construireCookieCsrfSuppression() {
        return ResponseCookie.from(NOM_COOKIE_CSRF, "")
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }

    public Optional<String> lireToken(HttpServletRequest request) {
        return lireCookie(request, NOM_COOKIE);
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
