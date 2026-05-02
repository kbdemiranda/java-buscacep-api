package io.github.kbdemiranda.zipcode.search.dto;

import io.github.kbdemiranda.zipcode.search.model.ZipCodeProvider;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeQueryStatus;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ZipCodeQueryLogFilterDTO {

    private final String zipCode;
    private final ZipCodeQueryStatus status;
    private final ZipCodeProvider provider;
    private final LocalDateTime dateFrom;
    private final LocalDateTime dateTo;

    public ZipCodeQueryLogFilterDTO(
        String zipCodeInput,
        ZipCodeQueryStatus status,
        ZipCodeProvider provider,
        LocalDateTime dateFrom,
        LocalDateTime dateTo
    ) {
        this.zipCode = normalizeZipCode(zipCodeInput);
        this.status = status;
        this.provider = provider;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    private String normalizeZipCode(String rawZipCode) {
        if (rawZipCode == null || rawZipCode.isBlank()) {
            return null;
        }
        String normalized = rawZipCode.replaceAll("\\D", "");
        return normalized.isBlank() ? null : normalized;
    }
}
