package io.github.kbdemiranda.zipcode.search.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kbdemiranda.zipcode.search.config.ZipCodeClientProperties;
import io.github.kbdemiranda.zipcode.search.dto.ViaCepResponseDTO;
import io.github.kbdemiranda.zipcode.search.exception.ExternalZipCodeClientException;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeProvider;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ViaCepZipCodeClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ViaCepZipCodeClient client;

    @BeforeEach
    void setUp() {
        client = new ViaCepZipCodeClient(restTemplate, new ZipCodeClientProperties("http://wiremock", "https://viacep.com.br/ws"));
    }

    @Test
    void shouldReturnMappedResponseWhenViaCepReturnsValidPayload() {
        ViaCepResponseDTO payload = new ViaCepResponseDTO(
            "01001-000",
            "Praça da Sé",
            "lado ímpar",
            "Sé",
            "São Paulo",
            "SP",
            false
        );
        when(restTemplate.getForObject("https://viacep.com.br/ws/{zipCode}/json", ViaCepResponseDTO.class, "01001000"))
            .thenReturn(payload);

        var result = client.searchZipCode("01001000");

        assertThat(result).isPresent();
        assertThat(result.get().zipCode()).isEqualTo("01001-000");
        assertThat(result.get().logradouro()).isEqualTo("Praça da Sé");
        assertThat(result.get().bairro()).isEqualTo("Sé");
        verify(restTemplate).getForObject("https://viacep.com.br/ws/{zipCode}/json", ViaCepResponseDTO.class, "01001000");
    }

    @Test
    void shouldReturnEmptyWhenViaCepReturnsErroFlag() {
        ViaCepResponseDTO payload = new ViaCepResponseDTO(null, null, null, null, null, null, true);
        when(restTemplate.getForObject("https://viacep.com.br/ws/{zipCode}/json", ViaCepResponseDTO.class, "00000000"))
            .thenReturn(payload);

        var result = client.searchZipCode("00000000");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenViaCepReturns404() {
        HttpClientErrorException notFound = HttpClientErrorException.create(
            HttpStatus.NOT_FOUND,
            "Not Found",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );
        when(restTemplate.getForObject("https://viacep.com.br/ws/{zipCode}/json", ViaCepResponseDTO.class, "99999999"))
            .thenThrow(notFound);

        var result = client.searchZipCode("99999999");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowExternalExceptionWhenViaCepReturnsOtherHttpError() {
        HttpClientErrorException badRequest = HttpClientErrorException.create(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );
        when(restTemplate.getForObject("https://viacep.com.br/ws/{zipCode}/json", ViaCepResponseDTO.class, "12345678"))
            .thenThrow(badRequest);

        assertThatThrownBy(() -> client.searchZipCode("12345678"))
            .isInstanceOf(ExternalZipCodeClientException.class)
            .hasMessage("ViaCEP call failed")
            .extracting(ex -> ((ExternalZipCodeClientException) ex).getProvider())
            .isEqualTo(ZipCodeProvider.VIACEP);
    }
}
