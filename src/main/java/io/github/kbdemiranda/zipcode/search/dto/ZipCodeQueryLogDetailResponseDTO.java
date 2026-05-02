package io.github.kbdemiranda.zipcode.search.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "Detailed CEP query log response")
public record ZipCodeQueryLogDetailResponseDTO(
    @Schema(description = "External identifier for the query log", example = "5dbf0be0-77ff-4c5d-a69f-d8452d58fbd2")
    UUID externalId,
    @JsonProperty("cep")
    @Schema(description = "Queried CEP", example = "04364-030")
    String zipCode,
    @Schema(description = "Provider used to resolve the CEP", example = "WIREMOCK")
    String provider,
    @Schema(description = "Query status", example = "SUCCESS")
    String status,
    @Schema(description = "Timestamp when the query was made", example = "2026-04-30T15:22:00")
    LocalDateTime requestTimestamp,
    @Schema(description = "Raw response body returned by provider")
    JsonNode responseBody,
    @Schema(description = "Error message in case of failed query", example = "WireMock timeout")
    String errorMessage,
    @Schema(description = "Record creation timestamp", example = "2026-04-30T15:22:00")
    LocalDateTime createdAt,
    @Schema(description = "Record last update timestamp", example = "2026-04-30T15:22:05")
    LocalDateTime updatedAt
) {
}
