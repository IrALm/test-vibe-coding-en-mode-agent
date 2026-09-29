package com.walsia.api_compta.authentification.repository;

import com.walsia.api_compta.authentification.entity.UserToken;
import com.walsia.api_compta.authentification.entity.UserTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, String> {

    Optional<UserToken> findByTokenHashAndType(String tokenHash, UserTokenType type);
}
