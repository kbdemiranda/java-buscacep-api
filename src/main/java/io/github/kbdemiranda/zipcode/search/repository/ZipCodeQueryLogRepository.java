package io.github.kbdemiranda.zipcode.search.repository;

import io.github.kbdemiranda.zipcode.search.model.ZipCodeQueryLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ZipCodeQueryLogRepository extends JpaRepository<ZipCodeQueryLog, Long>, JpaSpecificationExecutor<ZipCodeQueryLog> {
    Optional<ZipCodeQueryLog> findByExternalId(UUID externalId);
}
