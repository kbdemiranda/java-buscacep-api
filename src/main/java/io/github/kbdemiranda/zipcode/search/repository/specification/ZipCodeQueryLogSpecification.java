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

            if (filter.getZipCode() != null && !filter.getZipCode().isBlank()) {
                predicates.add(cb.equal(root.get("zipCode"), filter.getZipCode()));
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
