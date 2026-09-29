package com.walsia.api_compta.authentification.service.impl;

import com.walsia.api_compta.authentification.service.interfaces.TokenCipherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM. La clé est fournie en base64 (app.session.encryption-key) - une
 * rotation de clé invalide toutes les sessions actives (déchiffrement impossible).
 */
@Service
public class TokenCipherServiceImpl implements TokenCipherService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec cle;

    public TokenCipherServiceImpl(@Value("${app.session.encryption-key}") String encryptionKeyBase64) {
        byte[] cleBrute = Base64.getDecoder().decode(encryptionKeyBase64);
        this.cle = new SecretKeySpec(cleBrute, "AES");
    }

    @Override
    public String chiffrer(String valeurEnClair) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] chiffre = cipher.doFinal(valeurEnClair.getBytes(StandardCharsets.UTF_8));

            byte[] resultat = new byte[iv.length + chiffre.length];
            System.arraycopy(iv, 0, resultat, 0, iv.length);
            System.arraycopy(chiffre, 0, resultat, iv.length, chiffre.length);
            return Base64.getEncoder().encodeToString(resultat);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Échec du chiffrement du token", e);
        }
    }

    @Override
    public String dechiffrer(String valeurChiffree) {
        try {
            byte[] donnees = Base64.getDecoder().decode(valeurChiffree);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(donnees, 0, iv, 0, IV_LENGTH_BYTES);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] clair = cipher.doFinal(donnees, IV_LENGTH_BYTES, donnees.length - IV_LENGTH_BYTES);
            return new String(clair, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Échec du déchiffrement du token", e);
        }
    }
}
