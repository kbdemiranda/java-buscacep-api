package io.github.kbdemiranda.zipcode.search.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ViaCepResponseDTOTest {

    @Test
    void shouldExposeRecordFieldsAndEquality() {
        ViaCepResponseDTO response = new ViaCepResponseDTO(
            "01001-000",
            "Praça da Sé",
            "lado ímpar",
            "Sé",
            "São Paulo",
            "SP",
            false
        );

        assertThat(response.cep()).isEqualTo("01001-000");
        assertThat(response.logradouro()).isEqualTo("Praça da Sé");
        assertThat(response.complemento()).isEqualTo("lado ímpar");
        assertThat(response.bairro()).isEqualTo("Sé");
        assertThat(response.localidade()).isEqualTo("São Paulo");
        assertThat(response.uf()).isEqualTo("SP");
        assertThat(response.erro()).isFalse();

        ViaCepResponseDTO sameResponse = new ViaCepResponseDTO(
            "01001-000",
            "Praça da Sé",
            "lado ímpar",
            "Sé",
            "São Paulo",
            "SP",
            false
        );

        assertThat(response).isEqualTo(sameResponse);
        assertThat(response.hashCode()).isEqualTo(sameResponse.hashCode());
        assertThat(response).hasToString(sameResponse.toString());
    }
}
