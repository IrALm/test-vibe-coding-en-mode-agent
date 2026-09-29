package com.walsia.api_compta.authentification.controller;

import com.walsia.api_compta.authentification.dto.formDto.DefinirMotDePasseForm;
import com.walsia.api_compta.authentification.dto.formDto.ForgotPasswordForm;
import com.walsia.api_compta.authentification.dto.formDto.LoginForm;
import com.walsia.api_compta.authentification.dto.formDto.ResendVerificationForm;
import com.walsia.api_compta.authentification.dto.formDto.ResetPasswordForm;
import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurReadDto;
import com.walsia.api_compta.authentification.security.SessionCookieHelper;
import com.walsia.api_compta.authentification.service.interfaces.AuthAccountService;
import com.walsia.api_compta.authentification.service.interfaces.AuthSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthAccountService authAccountService;
    private final AuthSessionService authSessionService;
    private final SessionCookieHelper sessionCookieHelper;

    public AuthController(
            AuthAccountService authAccountService,
            AuthSessionService authSessionService,
            SessionCookieHelper sessionCookieHelper) {
        this.authAccountService = authAccountService;
        this.authSessionService = authSessionService;
        this.sessionCookieHelper = sessionCookieHelper;
    }

    @PostMapping("/login")
    public ResponseEntity<UtilisateurReadDto> login(@Valid @RequestBody LoginForm form) {
        AuthSessionService.SessionConnectee session = authSessionService.connecter(form.email(), form.motDePasse());
        String valeurCsrf = sessionCookieHelper.genererValeurCsrf();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookieHelper.construireCookieConnexion(session.tokenSessionEnClair()).toString())
                .header(HttpHeaders.SET_COOKIE, sessionCookieHelper.construireCookieCsrf(valeurCsrf).toString())
                .body(session.utilisateur());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        sessionCookieHelper.lireToken(request).ifPresent(authSessionService::deconnecter);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookieHelper.construireCookieDeconnexion().toString())
                .header(HttpHeaders.SET_COOKIE, sessionCookieHelper.construireCookieCsrfSuppression().toString())
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> demanderReinitialisation(@Valid @RequestBody ForgotPasswordForm form) {
        authAccountService.demanderReinitialisationMotDePasse(form.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> reinitialiserMotDePasse(@Valid @RequestBody ResetPasswordForm form) {
        authAccountService.reinitialiserMotDePasse(form.token(), form.nouveauMotDePasse());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifierEmail(@RequestParam String token) {
        authAccountService.verifierEmail(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> renvoyerVerification(@Valid @RequestBody ResendVerificationForm form) {
        authAccountService.renvoyerEmailVerification(form.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/definir-mot-de-passe")
    public ResponseEntity<Void> definirMotDePassePermanent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DefinirMotDePasseForm form) {
        authAccountService.definirMotDePassePermanent(jwt.getSubject(), form.nouveauMotDePasse());
        return ResponseEntity.noContent().build();
    }
}
