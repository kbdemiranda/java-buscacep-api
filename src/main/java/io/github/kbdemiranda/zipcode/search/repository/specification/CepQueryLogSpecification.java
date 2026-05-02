package io.github.kbdemiranda.zipcode.search.repository.specification;

import io.github.kbdemiranda.zipcode.search.dto.CepQueryLogFilterDTO;
import io.github.kbdemiranda.zipcode.search.model.CepQueryLog;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class CepQueryLogSpecification {

    private CepQueryLogSpecification() {
    }

    public static Specification<CepQueryLog> withFilters(CepQueryLogFilterDTO filter) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if (filter.getCep() != null && !filter.getCep().isBlank()) {
                predicates.add(cb.equal(root.get("cep"), filter.getCep()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getProvider() != null) {
                predicates.add(cb.equal(root.get("provider"), filter.getProvider()));
            }
            if (filter.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestTimestamp"), filter.getDateFrom()));
            }
            if (filter.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("requestTimestamp"), filter.getDateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
