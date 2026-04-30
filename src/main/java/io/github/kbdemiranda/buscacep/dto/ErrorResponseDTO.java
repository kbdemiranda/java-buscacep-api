package io.github.kbdemiranda.buscacep.dto;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
    int status,
    String message,
    LocalDateTime timestamp
) {
}
