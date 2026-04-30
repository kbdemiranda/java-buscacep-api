package io.github.kbdemiranda.buscacep.dto;

import lombok.Builder;

@Builder
public record CepResponseDTO(
    String cep,
    String logradouro,
    String complemento,
    String bairro,
    String localidade,
    String uf
) {
}
