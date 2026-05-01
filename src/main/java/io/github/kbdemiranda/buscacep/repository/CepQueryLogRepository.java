package io.github.kbdemiranda.buscacep.repository;

import io.github.kbdemiranda.buscacep.model.CepQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CepQueryLogRepository extends JpaRepository<CepQueryLog, Long>, JpaSpecificationExecutor<CepQueryLog> {
}
