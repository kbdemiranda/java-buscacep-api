package io.github.kbdemiranda.buscacep.client;

import io.github.kbdemiranda.buscacep.dto.CepResponseDTO;
import io.github.kbdemiranda.buscacep.exception.ExternalCepClientException;
import io.github.kbdemiranda.buscacep.config.CepClientProperties;
import io.github.kbdemiranda.buscacep.model.CepProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class WiremockCepClient implements CepClient {

    private final RestTemplate restTemplate;
    private final CepClientProperties cepClientProperties;

    @Override
    public Optional<CepResponseDTO> findByCep(String cep) {
        String url = cepClientProperties.wiremockUrl() + "/cep/{cep}";
        try {
            CepResponseDTO response = restTemplate.getForObject(url, CepResponseDTO.class, cep);
            return Optional.ofNullable(response);
        } catch (HttpClientErrorException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.value() == 404) {
                return Optional.empty();
            }
            throw new ExternalCepClientException(CepProvider.WIREMOCK, "WireMock call failed", e);
        } catch (Exception e) {
            throw new ExternalCepClientException(CepProvider.WIREMOCK, "WireMock call failed", e);
        }
    }
}
