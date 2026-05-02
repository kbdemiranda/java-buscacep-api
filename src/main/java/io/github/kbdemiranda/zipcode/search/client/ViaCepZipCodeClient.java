package io.github.kbdemiranda.zipcode.search.client;

import io.github.kbdemiranda.zipcode.search.dto.ZipCodeResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.ViaCepResponseDTO;
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
public class ViaCepZipCodeClient implements ZipCodeClient {

    private final RestTemplate restTemplate;
    private final ZipCodeClientProperties zipCodeClientProperties;

    @Override
    public Optional<ZipCodeResponseDTO> searchZipCode(String zipCode) {
        String url = zipCodeClientProperties.viacepUrl() + "/{zipCode}/json";
        try {
            ViaCepResponseDTO response = restTemplate.getForObject(url, ViaCepResponseDTO.class, zipCode);
            if (response == null) {
                return Optional.empty();
            }
            if (Boolean.TRUE.equals(response.erro())) {
                return Optional.empty();
            }
            if (response.cep() == null || response.logradouro() == null) {
                return Optional.empty();
            }

            ZipCodeResponseDTO dto = ZipCodeResponseDTO.builder()
                .zipCode(response.cep())
                .logradouro(response.logradouro())
                .complemento(response.complemento())
                .bairro(response.bairro())
                .localidade(response.localidade())
                .uf(response.uf())
                .build();
            return Optional.of(dto);
        } catch (HttpClientErrorException e) {
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.value() == 404) {
                return Optional.empty();
            }
            throw new ExternalZipCodeClientException(ZipCodeProvider.VIACEP, "ViaCEP call failed", e);
        } catch (Exception e) {
            throw new ExternalZipCodeClientException(ZipCodeProvider.VIACEP, "ViaCEP call failed", e);
        }
    }
}
