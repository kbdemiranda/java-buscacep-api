package io.github.kbdemiranda.zipcode.search.dto;

import io.github.kbdemiranda.zipcode.search.model.ZipCodeProvider;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeQueryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Filters for CEP query log search")
public record ZipCodeQueryLogFilterDTO(
        @Schema(description = "CEP used to filter query logs")
        String zipCode,
        @Schema(description = "Query status used as filter")
        ZipCodeQueryStatus status,
        @Schema(description = "Provider used as filter")
        ZipCodeProvider provider,
        @Schema(description = "Start date-time for filtering query timestamp")
        LocalDateTime dateFrom,
        @Schema(description = "End date-time for filtering query timestamp")
        LocalDateTime dateTo
) {

    public ZipCodeQueryLogFilterDTO(
            String zipCode,
            ZipCodeQueryStatus status,
            ZipCodeProvider provider,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        this.zipCode = normalizeZipCode(zipCode);
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
