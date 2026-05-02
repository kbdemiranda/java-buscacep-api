package io.github.kbdemiranda.zipcode.search.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kbdemiranda.zipcode.search.config.ZipCodeClientProperties;
import io.github.kbdemiranda.zipcode.search.dto.ZipCodeResponseDTO;
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
class WireMockZipCodeClientTest {

    @Mock
    private RestTemplate restTemplate;

    private WireMockZipCodeClient client;

    @BeforeEach
    void setUp() {
        client = new WireMockZipCodeClient(restTemplate, new ZipCodeClientProperties("http://localhost:8089", "https://viacep.com.br/ws"));
    }

    @Test
    void shouldReturnResponseWhenWireMockReturnsPayload() {
        ZipCodeResponseDTO payload = ZipCodeResponseDTO.builder()
            .zipCode("04364-030")
            .logradouro("Rua Exemplo")
            .bairro("Jabaquara")
            .localidade("São Paulo")
            .uf("SP")
            .build();
        when(restTemplate.getForObject("http://localhost:8089/cep/{zipCode}", ZipCodeResponseDTO.class, "04364030"))
            .thenReturn(payload);

        var result = client.searchZipCode("04364030");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(payload);
        verify(restTemplate).getForObject("http://localhost:8089/cep/{zipCode}", ZipCodeResponseDTO.class, "04364030");
    }

    @Test
    void shouldReturnEmptyWhenWireMockReturnsNull() {
        when(restTemplate.getForObject("http://localhost:8089/cep/{zipCode}", ZipCodeResponseDTO.class, "00000000"))
            .thenReturn(null);

        var result = client.searchZipCode("00000000");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenWireMockReturns404() {
        HttpClientErrorException notFound = HttpClientErrorException.create(
            HttpStatus.NOT_FOUND,
            "Not Found",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );
        when(restTemplate.getForObject("http://localhost:8089/cep/{zipCode}", ZipCodeResponseDTO.class, "99999999"))
            .thenThrow(notFound);

        var result = client.searchZipCode("99999999");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowExternalExceptionWhenWireMockReturnsOtherHttpError() {
        HttpClientErrorException badGateway = HttpClientErrorException.create(
            HttpStatus.BAD_GATEWAY,
            "Bad Gateway",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );
        when(restTemplate.getForObject("http://localhost:8089/cep/{zipCode}", ZipCodeResponseDTO.class, "12345678"))
            .thenThrow(badGateway);

        assertThatThrownBy(() -> client.searchZipCode("12345678"))
            .isInstanceOf(ExternalZipCodeClientException.class)
            .hasMessage("WireMock call failed")
            .extracting(ex -> ((ExternalZipCodeClientException) ex).getProvider())
            .isEqualTo(ZipCodeProvider.WIREMOCK);
    }
}
