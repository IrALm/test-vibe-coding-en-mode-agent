package com.walsia.compta.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CredentialEncryptorTest {

    private CredentialEncryptor credentialEncryptor;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        credentialEncryptor = new CredentialEncryptor(Base64.getEncoder().encodeToString(key));
    }

    @Test
    void encryptThenDecrypt_returnsOriginalPlainText() {
        String plainText = "S3cr3t-P@ssw0rd!";

        String encrypted = credentialEncryptor.encrypt(plainText);
        String decrypted = credentialEncryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plainText);
    }

    @Test
    void encrypt_producesDifferentCiphertextsForSamePlainText() {
        String plainText = "S3cr3t-P@ssw0rd!";

        String encryptedFirst = credentialEncryptor.encrypt(plainText);
        String encryptedSecond = credentialEncryptor.encrypt(plainText);

        assertThat(encryptedFirst).isNotEqualTo(encryptedSecond);
        assertThat(credentialEncryptor.decrypt(encryptedFirst)).isEqualTo(plainText);
        assertThat(credentialEncryptor.decrypt(encryptedSecond)).isEqualTo(plainText);
    }

    @Test
    void constructor_rejectsMasterKeyWithWrongLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new CredentialEncryptor(shortKey))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsBlankMasterKey() {
        assertThatThrownBy(() -> new CredentialEncryptor(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decrypt_rejectsTamperedCiphertext() {
        String encrypted = credentialEncryptor.encrypt("valeur-initiale");
        byte[] raw = Base64.getDecoder().decode(encrypted);
        raw[raw.length - 1] ^= 0x01; // corrompt le dernier octet (tag GCM)
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> credentialEncryptor.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class);
    }
}
