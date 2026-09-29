package com.walsia.compta.config;

import com.walsia.compta.crypto.CredentialEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    @Bean
    public CredentialEncryptor credentialEncryptor(@Value("${compta.crypto.master-key}") String masterKey) {
        return new CredentialEncryptor(masterKey);
    }
}
