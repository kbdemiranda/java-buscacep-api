package io.github.kbdemiranda.zipcode.search.repository;

import io.github.kbdemiranda.zipcode.search.model.CepQueryLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CepQueryLogRepository extends JpaRepository<CepQueryLog, Long>, JpaSpecificationExecutor<CepQueryLog> {
    Optional<CepQueryLog> findByExternalId(UUID externalId);
}
