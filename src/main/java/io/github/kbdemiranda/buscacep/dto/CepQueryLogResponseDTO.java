package io.github.kbdemiranda.buscacep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "CEP query log response")
public record CepQueryLogResponseDTO(
    @Schema(description = "External identifier for the query log", example = "5dbf0be0-77ff-4c5d-a69f-d8452d58fbd2")
    UUID externalId,
    @Schema(description = "Queried CEP", example = "04364-030")
    String cep,
    @Schema(description = "Provider used to resolve the CEP", example = "WIREMOCK")
    String provider,
    @Schema(description = "Query status", example = "SUCCESS")
    String status,
    @Schema(description = "Timestamp when the query was made", example = "2026-04-30T15:22:00")
    LocalDateTime requestTimestamp
) {
}
