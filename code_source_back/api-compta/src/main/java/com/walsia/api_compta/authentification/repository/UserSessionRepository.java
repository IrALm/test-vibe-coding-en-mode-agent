package com.walsia.api_compta.authentification.repository;

import com.walsia.api_compta.authentification.entity.UserSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    Optional<UserSession> findBySessionTokenHashAndRevokedAtIsNull(String sessionTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserSession s where s.sessionTokenHash = :sessionTokenHash and s.revokedAt is null")
    Optional<UserSession> findBySessionTokenHashAndRevokedAtIsNullForUpdate(String sessionTokenHash);

    List<UserSession> findAllByUtilisateur_IdAndRevokedAtIsNull(String utilisateurId);
}
