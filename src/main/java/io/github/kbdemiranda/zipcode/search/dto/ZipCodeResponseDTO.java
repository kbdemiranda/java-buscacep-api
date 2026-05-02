package io.github.kbdemiranda.zipcode.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "CEP lookup response")
public record ZipCodeResponseDTO(
    @JsonProperty("cep")
    @Schema(description = "CEP in formatted representation", example = "04364-030")
    String zipCode,
    @Schema(description = "Street name", example = "Rua das Flechas")
    String logradouro,
    @Schema(description = "Address complement", example = "de 1000 a 1598 - lado par")
    String complemento,
    @Schema(description = "District", example = "Vila Santa Catarina")
    String bairro,
    @Schema(description = "City", example = "São Paulo")
    String localidade,
    @Schema(description = "Federative unit", example = "SP")
    String uf
) {
}
