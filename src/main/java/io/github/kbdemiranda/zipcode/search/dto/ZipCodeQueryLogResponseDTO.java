package io.github.kbdemiranda.zipcode.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "CEP query log response")
public record ZipCodeQueryLogResponseDTO(
    @Schema(description = "External identifier for the query log")
    UUID externalId,
    @JsonProperty("cep")
    @Schema(description = "Queried CEP")
    String zipCode,
    @Schema(description = "Provider used to resolve the CEP")
    String provider,
    @Schema(description = "Query status")
    String status,
    @Schema(description = "Timestamp when the query was made")
    LocalDateTime requestTimestamp
) {
}
