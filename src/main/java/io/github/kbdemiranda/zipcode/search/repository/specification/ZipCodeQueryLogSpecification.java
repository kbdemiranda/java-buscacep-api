package io.github.kbdemiranda.zipcode.search.repository.specification;

import io.github.kbdemiranda.zipcode.search.dto.ZipCodeQueryLogFilterDTO;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeQueryLog;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class ZipCodeQueryLogSpecification {

    private ZipCodeQueryLogSpecification() {
    }

    public static Specification<ZipCodeQueryLog> withFilters(ZipCodeQueryLogFilterDTO filter) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if (filter.zipCode() != null && !filter.zipCode().isBlank()) {
                predicates.add(cb.equal(root.get("zipCode"), filter.zipCode()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.provider() != null) {
                predicates.add(cb.equal(root.get("provider"), filter.provider()));
            }
            if (filter.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestTimestamp"), filter.dateFrom()));
            }
            if (filter.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("requestTimestamp"), filter.dateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
