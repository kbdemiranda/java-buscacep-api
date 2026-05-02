package io.github.kbdemiranda.zipcode.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "Detailed CEP query log response")
public record ZipCodeQueryLogDetailResponseDTO(
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
    LocalDateTime requestTimestamp,
    @Schema(description = "Raw response body returned by provider")
    Map<String, Object> responseBody,
    @Schema(description = "Error message in case of failed query")
    String errorMessage,
    @Schema(description = "Record creation timestamp")
    LocalDateTime createdAt,
    @Schema(description = "Record last update timestamp")
    LocalDateTime updatedAt
) {
}
