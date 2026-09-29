package com.walsia.api_compta.authentification.service.impl;

import com.walsia.api_compta.authentification.entity.UserToken;
import com.walsia.api_compta.authentification.entity.UserTokenType;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import com.walsia.api_compta.exception.TokenInvalideException;
import com.walsia.api_compta.authentification.repository.UserTokenRepository;
import com.walsia.api_compta.authentification.service.interfaces.UserTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class UserTokenServiceImpl implements UserTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserTokenRepository userTokenRepository;

    public UserTokenServiceImpl(UserTokenRepository userTokenRepository) {
        this.userTokenRepository = userTokenRepository;
    }

    @Override
    @Transactional
    public String genererToken(Utilisateur utilisateur, UserTokenType type) {
        String tokenEnClair = genererTokenAleatoire();
        UserToken userToken = UserToken.builder()
                .utilisateur(utilisateur)
                .tokenHash(hacher(tokenEnClair))
                .type(type)
                .expiresAt(LocalDateTime.now().plusHours(dureeValiditeHeures(type)))
                .createdAt(LocalDateTime.now())
                .build();
        userTokenRepository.save(userToken);
        return tokenEnClair;
    }

    @Override
    @Transactional
    public Utilisateur consommerToken(String tokenEnClair, UserTokenType type) {
        UserToken userToken = userTokenRepository.findByTokenHashAndType(hacher(tokenEnClair), type)
                .orElseThrow(() -> new TokenInvalideException("Token invalide"));

        if (userToken.getUsedAt() != null) {
            throw new TokenInvalideException("Token déjà utilisé");
        }
        if (userToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenInvalideException("Token expiré");
        }

        userToken.setUsedAt(LocalDateTime.now());
        userTokenRepository.save(userToken);
        return userToken.getUtilisateur();
    }

    private long dureeValiditeHeures(UserTokenType type) {
        return switch (type) {
            case EMAIL_VERIFICATION -> 24L;
            case PASSWORD_RESET -> 1L;
        };
    }

    private String genererTokenAleatoire() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hacher(String valeur) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valeur.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
