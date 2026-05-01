package io.github.kbdemiranda.buscacep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Standard error response")
public record ErrorResponseDTO(
    @Schema(description = "Error timestamp", example = "2026-04-30T15:22:00")
    LocalDateTime timestamp,
    @Schema(description = "HTTP status code", example = "400")
    int status,
    @Schema(description = "HTTP reason phrase", example = "Bad Request")
    String error,
    @Schema(description = "User-friendly error message", example = "CEP must have 8 digits or format 99999-999")
    String message,
    @Schema(description = "Request path", example = "/api/v1/ceps/00000000")
    String path
) {
}
