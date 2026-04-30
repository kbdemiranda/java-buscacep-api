package io.github.kbdemiranda.buscacep.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CepQueryLogResponseDTO(
    UUID externalId,
    String cep,
    String provider,
    String status,
    LocalDateTime requestTimestamp
) {
}
