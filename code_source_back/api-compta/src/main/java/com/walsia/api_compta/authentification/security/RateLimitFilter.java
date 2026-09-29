package com.walsia.api_compta.authentification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting basique en mémoire (par IP, fenêtre fixe), sur les endpoints publics
 * les plus exposés aux abus (credential stuffing, email bombing, énumération de
 * comptes). Basé sur request.getRemoteAddr() uniquement - ne fait pas confiance à
 * X-Forwarded-For tant qu'il n'y a pas de reverse proxy de confiance devant l'appli
 * (sinon un client pourrait usurper son IP et contourner la limite).
 *
 * Limite connue : compteur local au process, donc inefficace si l'app tourne un jour
 * sur plusieurs instances (nécessiterait alors un compteur partagé, ex. Redis).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Regle(String methode, String chemin, int maxRequetes, Duration fenetre) {}

    private static final Regle[] REGLES = {
            new Regle("POST", "/api/auth/login", 20, Duration.ofMinutes(5)),
            new Regle("POST", "/api/auth/forgot-password", 5, Duration.ofMinutes(15)),
            new Regle("POST", "/api/auth/resend-verification", 5, Duration.ofMinutes(15)),
            new Regle("POST", "/api/entites", 10, Duration.ofHours(1)),
    };

    private static class Fenetre {
        final AtomicInteger compte = new AtomicInteger(0);
        volatile Instant expiration;

        Fenetre(Instant expiration) {
            this.expiration = expiration;
        }
    }

    private final Map<String, Fenetre> compteurs = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        for (Regle regle : REGLES) {
            if (regle.methode().equals(request.getMethod()) && regle.chemin().equals(request.getRequestURI())) {
                String cle = regle.chemin() + "|" + request.getRemoteAddr();
                if (depasseLimite(cle, regle)) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\":\"Trop de requêtes, réessayez plus tard\"}");
                    return;
                }
                break;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean depasseLimite(String cle, Regle regle) {
        Instant maintenant = Instant.now();
        Fenetre fenetre = compteurs.computeIfAbsent(cle, k -> new Fenetre(maintenant.plus(regle.fenetre())));

        synchronized (fenetre) {
            if (maintenant.isAfter(fenetre.expiration)) {
                fenetre.compte.set(0);
                fenetre.expiration = maintenant.plus(regle.fenetre());
            }
            return fenetre.compte.incrementAndGet() > regle.maxRequetes();
        }
    }
}
