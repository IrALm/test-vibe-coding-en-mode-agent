package com.walsia.compta.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Chiffrement/déchiffrement des identifiants (mots de passe de connexion aux
 * bases tenant) stockés dans l'annuaire, en AES-256-GCM.
 * <p>
 * Format du texte chiffré stocké (base64) : {@code IV(12 octets) || ciphertext+tag}.
 * <p>
 * Important : cette classe ne doit jamais logger la clé maître ni le texte
 * en clair (mots de passe).
 */
public class CredentialEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BYTES = 32;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialEncryptor(String masterKeyBase64) {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            throw new IllegalArgumentException("COMPTA_MASTER_KEY est requis (clé AES-256 encodée en base64)");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(masterKeyBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("COMPTA_MASTER_KEY doit être encodé en base64", e);
        }
        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "COMPTA_MASTER_KEY doit correspondre à une clé AES-256 (32 octets une fois décodée), trouvé "
                            + keyBytes.length + " octets");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Chiffre le texte en clair fourni et retourne un texte encodé en base64
     * prêt à être stocké (IV préfixé au ciphertext).
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            throw new IllegalArgumentException("La valeur à chiffrer ne peut pas être nulle");
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] output = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(cipherText, 0, output, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(output);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Erreur lors du chiffrement des identifiants tenant", e);
        }
    }

    /**
     * Déchiffre une valeur précédemment produite par {@link #encrypt(String)}.
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null) {
            throw new IllegalArgumentException("La valeur à déchiffrer ne peut pas être nulle");
        }
        try {
            byte[] input = Base64.getDecoder().decode(encryptedBase64);
            if (input.length < IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Valeur chiffrée invalide (trop courte)");
            }
            byte[] iv = Arrays.copyOfRange(input, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(input, IV_LENGTH_BYTES, input.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Erreur lors du déchiffrement des identifiants tenant", e);
        }
    }
}
