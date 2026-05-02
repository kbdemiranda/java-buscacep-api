package io.github.kbdemiranda.zipcode.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "CEP lookup response")
public record ZipCodeResponseDTO(
    @JsonProperty("cep")
    @Schema(description = "CEP in formatted representation")
    String zipCode,
    @Schema(description = "Street name")
    String logradouro,
    @Schema(description = "Address complement")
    String complemento,
    @Schema(description = "District")
    String bairro,
    @Schema(description = "City")
    String localidade,
    @Schema(description = "Federative unit")
    String uf
) {
}
