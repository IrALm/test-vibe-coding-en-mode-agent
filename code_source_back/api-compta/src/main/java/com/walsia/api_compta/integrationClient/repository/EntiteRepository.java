package com.walsia.api_compta.integrationClient.repository;

import com.walsia.api_compta.integrationClient.entity.entite.Entite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntiteRepository extends JpaRepository<Entite, String> {
}
