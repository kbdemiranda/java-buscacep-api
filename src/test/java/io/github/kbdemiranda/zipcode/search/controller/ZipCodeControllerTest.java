package io.github.kbdemiranda.zipcode.search.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kbdemiranda.zipcode.search.dto.ZipCodeResponseDTO;
import io.github.kbdemiranda.zipcode.search.exception.ZipCodeNotFoundException;
import io.github.kbdemiranda.zipcode.search.exception.ExternalZipCodeClientException;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeProvider;
import io.github.kbdemiranda.zipcode.search.service.ZipCodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ZipCodeController.class)
@Import(GlobalExceptionHandler.class)
class ZipCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZipCodeService zipCodeService;

    @ParameterizedTest
    @CsvSource({
        "04364030,04364-030",
        "01001000,01001-000",
        "30140071,30140-071"
    })
    void shouldReturn200ForKnownZipCodes(String inputZipCode, String responseCep) throws Exception {
        when(zipCodeService.searchZipCode(inputZipCode)).thenReturn(
            ZipCodeResponseDTO.builder()
                .zipCode(responseCep)
                .logradouro("Rua de teste")
                .localidade("Cidade")
                .uf("SP")
                .build()
        );

        mockMvc.perform(get("/api/v1/zip-codes/{cep}", inputZipCode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cep").value(responseCep))
            .andExpect(jsonPath("$.logradouro").exists())
            .andExpect(jsonPath("$.localidade").exists())
            .andExpect(jsonPath("$.uf").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12AB5678"})
    void shouldReturn400ForInvalidZipCode(String invalidZipCode) throws Exception {
        mockMvc.perform(get("/api/v1/zip-codes/{cep}", invalidZipCode))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/zip-codes/" + invalidZipCode));

        verifyNoInteractions(zipCodeService);
    }

    @Test
    void shouldReturn404WithMessageWhenZipCodeIsNotFound() throws Exception {
        when(zipCodeService.searchZipCode("00000000")).thenThrow(new ZipCodeNotFoundException("00000000"));

        mockMvc.perform(get("/api/v1/zip-codes/{cep}", "00000000"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("CEP não encontrado"))
            .andExpect(jsonPath("$.path").value("/api/v1/zip-codes/00000000"));
    }

    @Test
    void shouldReturn502ForExternalProviderError() throws Exception {
        ExternalZipCodeClientException error = new ExternalZipCodeClientException(
            ZipCodeProvider.WIREMOCK,
            "WireMock timeout",
            new RuntimeException("timeout")
        );
        when(zipCodeService.searchZipCode("04364030")).thenThrow(error);

        mockMvc.perform(get("/api/v1/zip-codes/{cep}", "04364030"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(502))
            .andExpect(jsonPath("$.error").value("Bad Gateway"))
            .andExpect(jsonPath("$.message").value("Falha ao consultar provedor externo"))
            .andExpect(jsonPath("$.path").value("/api/v1/zip-codes/04364030"));
    }

    @Test
    void shouldReturn500ForUnexpectedError() throws Exception {
        when(zipCodeService.searchZipCode("04364030")).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/zip-codes/{cep}", "04364030"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
            .andExpect(jsonPath("$.message").value("Erro interno inesperado"))
            .andExpect(jsonPath("$.path").value("/api/v1/zip-codes/04364030"));
    }
}
