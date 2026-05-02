package io.github.kbdemiranda.zipcode.search.client;

import io.github.kbdemiranda.zipcode.search.dto.ZipCodeResponseDTO;
import io.github.kbdemiranda.zipcode.search.exception.ExternalZipCodeClientException;
import io.github.kbdemiranda.zipcode.search.config.ZipCodeClientProperties;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class WireMockZipCodeClient implements ZipCodeClient {

    private final RestTemplate restTemplate;
    private final ZipCodeClientProperties zipCodeClientProperties;

    @Override
    public Optional<ZipCodeResponseDTO> searchZipCode(String zipCode) {
        String url = zipCodeClientProperties.wiremockUrl() + "/cep/{zipCode}";
        try {
            ZipCodeResponseDTO response = restTemplate.getForObject(url, ZipCodeResponseDTO.class, zipCode);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.value() == 404) {
                return Optional.empty();
            }
            throw new ExternalZipCodeClientException(ZipCodeProvider.WIREMOCK, "WireMock call failed", e);
        } catch (Exception e) {
            throw new ExternalZipCodeClientException(ZipCodeProvider.WIREMOCK, "WireMock call failed", e);
        }
    }
}
