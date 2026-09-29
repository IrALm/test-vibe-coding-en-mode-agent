package com.walsia.api_compta.integrationClient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walsia.api_compta.authentification.controller.AuthController;
import com.walsia.api_compta.integrationClient.dto.readDto.UtilisateurReadDto;
import com.walsia.api_compta.exception.AuthentificationEchoueeException;
import com.walsia.api_compta.authentification.security.SessionCookieHelper;
import com.walsia.api_compta.authentification.service.interfaces.AuthAccountService;
import com.walsia.api_compta.authentification.service.interfaces.AuthSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthAccountService authAccountService;
    @MockitoBean
    private AuthSessionService authSessionService;
    @MockitoBean
    private SessionCookieHelper sessionCookieHelper;
    // Dépendances de SessionCookieAuthenticationFilter (bean Filter, donc inclus dans le slice @WebMvcTest
    // même avec addFilters=false) sans lien avec ce test de contrôleur - juste à satisfaire pour le contexte.
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    @WithMockUser
    void login_succes_poseLeCookieEtNeRenvoieQueLesChampsDePorte() throws Exception {
        // DTO volontairement partiel, comme le construit réellement AuthSessionServiceImpl.connecter :
        // seuls les champs pilotant la redirection post-login sont peuplés, pas le profil complet.
        UtilisateurReadDto dto = new UtilisateurReadDto(null, null, null, null, null, null, false, true, false, null, null);
        when(authSessionService.connecter("jane@doe.com", "secret"))
                .thenReturn(new AuthSessionService.SessionConnectee(dto, "token-opaque"));
        when(sessionCookieHelper.construireCookieConnexion("token-opaque"))
                .thenReturn(org.springframework.http.ResponseCookie.from(SessionCookieHelper.NOM_COOKIE, "token-opaque")
                        .httpOnly(true).build());
        when(sessionCookieHelper.genererValeurCsrf()).thenReturn("csrf-genere");
        when(sessionCookieHelper.construireCookieCsrf("csrf-genere"))
                .thenReturn(org.springframework.http.ResponseCookie.from(SessionCookieHelper.NOM_COOKIE_CSRF, "csrf-genere")
                        .httpOnly(false).build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("jane@doe.com", "secret"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString(SessionCookieHelper.NOM_COOKIE)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.emailVerifie").value(true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.motDePasseTemporaire").value(false))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("token-opaque"))));
    }

    @Test
    @WithMockUser
    void login_identifiantsInvalides_renvoie401() throws Exception {
        when(authSessionService.connecter(anyString(), anyString()))
                .thenThrow(new AuthentificationEchoueeException("Identifiants invalides"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("jane@doe.com", "mauvais"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void logout_avecCookieAbsent_estIdempotentEtEffaceLeCookie() throws Exception {
        when(sessionCookieHelper.lireToken(any())).thenReturn(java.util.Optional.empty());
        when(sessionCookieHelper.construireCookieDeconnexion())
                .thenReturn(org.springframework.http.ResponseCookie.from(SessionCookieHelper.NOM_COOKIE, "")
                        .httpOnly(true).maxAge(0).build());
        when(sessionCookieHelper.construireCookieCsrfSuppression())
                .thenReturn(org.springframework.http.ResponseCookie.from(SessionCookieHelper.NOM_COOKIE_CSRF, "")
                        .httpOnly(false).maxAge(0).build());

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(authSessionService, never()).deconnecter(anyString());
    }

    private record LoginPayload(String email, String motDePasse) {}
}
