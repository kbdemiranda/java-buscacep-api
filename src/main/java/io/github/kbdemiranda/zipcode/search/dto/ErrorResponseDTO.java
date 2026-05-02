package io.github.kbdemiranda.zipcode.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Standard error response")
public record ErrorResponseDTO(
    @Schema(description = "Error timestamp")
    LocalDateTime timestamp,
    @Schema(description = "HTTP status code")
    int status,
    @Schema(description = "HTTP reason phrase")
    String error,
    @Schema(description = "User-friendly error message")
    String message,
    @Schema(description = "Request path")
    String path
) {
}
