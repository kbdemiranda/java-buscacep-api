package io.github.kbdemiranda.buscacep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Standard error response")
public record ErrorResponseDTO(
    @Schema(description = "HTTP status code", example = "400")
    int status,
    @Schema(description = "Error message", example = "Invalid CEP: 123")
    String message,
    @Schema(description = "Error timestamp", example = "2026-04-30T15:22:00")
    LocalDateTime timestamp
) {
}
