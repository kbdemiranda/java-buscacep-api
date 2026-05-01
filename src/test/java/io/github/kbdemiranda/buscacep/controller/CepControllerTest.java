package io.github.kbdemiranda.buscacep.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kbdemiranda.buscacep.dto.CepResponseDTO;
import io.github.kbdemiranda.buscacep.exception.CepNotFoundException;
import io.github.kbdemiranda.buscacep.service.CepService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CepController.class)
@Import(RestExceptionHandler.class)
class CepControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CepService cepService;

    @ParameterizedTest
    @CsvSource({
        "04364030,04364-030",
        "01001000,01001-000",
        "30140071,30140-071"
    })
    void shouldReturn200ForKnownCeps(String inputCep, String responseCep) throws Exception {
        when(cepService.findCep(inputCep)).thenReturn(
            CepResponseDTO.builder()
                .cep(responseCep)
                .logradouro("Rua de teste")
                .localidade("Cidade")
                .uf("SP")
                .build()
        );

        mockMvc.perform(get("/api/v1/ceps/{cep}", inputCep))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cep").value(responseCep))
            .andExpect(jsonPath("$.logradouro").exists())
            .andExpect(jsonPath("$.localidade").exists())
            .andExpect(jsonPath("$.uf").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12AB5678"})
    void shouldReturn400ForInvalidCep(String invalidCep) throws Exception {
        mockMvc.perform(get("/api/v1/ceps/{cep}", invalidCep))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(cepService);
    }

    @org.junit.jupiter.api.Test
    void shouldReturn404WithMessageWhenCepIsNotFound() throws Exception {
        when(cepService.findCep("00000000")).thenThrow(new CepNotFoundException("00000000"));

        mockMvc.perform(get("/api/v1/ceps/{cep}", "00000000"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("CEP não encontrado"));
    }
}
