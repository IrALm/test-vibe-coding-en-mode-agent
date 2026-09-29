package com.walsia.api_compta.integrationClient.repository;

import com.walsia.api_compta.integrationClient.entity.utilisateur.Role;
import com.walsia.api_compta.integrationClient.entity.utilisateur.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, String>, JpaSpecificationExecutor<Utilisateur> {

    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

    long countByEntite_IdAndRoleAndActif(String entiteId, Role role, boolean actif);
}
