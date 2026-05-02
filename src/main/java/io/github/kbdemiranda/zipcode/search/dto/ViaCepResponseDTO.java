package io.github.kbdemiranda.zipcode.search.dto;

public record ViaCepResponseDTO(
    String cep,
    String logradouro,
    String complemento,
    String bairro,
    String localidade,
    String uf,
    Boolean erro
) {
}
