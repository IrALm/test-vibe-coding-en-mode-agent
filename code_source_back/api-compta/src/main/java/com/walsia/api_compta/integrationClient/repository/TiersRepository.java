package com.walsia.api_compta.integrationClient.repository;

import com.walsia.api_compta.integrationClient.entity.tiers.Tiers;
import com.walsia.api_compta.integrationClient.entity.tiers.TypeTiers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TiersRepository extends JpaRepository<Tiers, String>, JpaSpecificationExecutor<Tiers> {

    long countByEntite_Id(String entiteId);

    long countByEntite_IdAndType(String entiteId, TypeTiers type);

    long countByEntite_IdAndCompteAssocieIsNull(String entiteId);
}
