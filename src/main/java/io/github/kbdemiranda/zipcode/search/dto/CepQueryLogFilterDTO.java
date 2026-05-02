package io.github.kbdemiranda.zipcode.search.dto;

import io.github.kbdemiranda.zipcode.search.model.CepProvider;
import io.github.kbdemiranda.zipcode.search.model.CepQueryStatus;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class CepQueryLogFilterDTO {

    private final String cep;
    private final CepQueryStatus status;
    private final CepProvider provider;
    private final LocalDateTime dateFrom;
    private final LocalDateTime dateTo;

    public CepQueryLogFilterDTO(
        String cep,
        CepQueryStatus status,
        CepProvider provider,
        LocalDateTime dateFrom,
        LocalDateTime dateTo
    ) {
        this.cep = normalizeCep(cep);
        this.status = status;
        this.provider = provider;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    private String normalizeCep(String rawCep) {
        if (rawCep == null || rawCep.isBlank()) {
            return null;
        }
        String normalized = rawCep.replaceAll("\\D", "");
        return normalized.isBlank() ? null : normalized;
    }
}
