package io.github.kbdemiranda.zipcode.search.client;

import io.github.kbdemiranda.zipcode.search.dto.CepResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.ViaCepResponseDTO;
import io.github.kbdemiranda.zipcode.search.exception.ExternalCepClientException;
import io.github.kbdemiranda.zipcode.search.config.CepClientProperties;
import io.github.kbdemiranda.zipcode.search.model.CepProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ViaCepClient implements CepClient {

    private final RestTemplate restTemplate;
    private final CepClientProperties cepClientProperties;

    @Override
    public Optional<CepResponseDTO> findByCep(String cep) {
        String url = cepClientProperties.viacepUrl() + "/{cep}/json";
        try {
            ViaCepResponseDTO response = restTemplate.getForObject(url, ViaCepResponseDTO.class, cep);
            if (response == null) {
                return Optional.empty();
            }
            if (Boolean.TRUE.equals(response.erro())) {
                return Optional.empty();
            }
            if (response.cep() == null || response.logradouro() == null) {
                return Optional.empty();
            }

            CepResponseDTO dto = CepResponseDTO.builder()
                .cep(response.cep())
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
            throw new ExternalCepClientException(CepProvider.VIACEP, "ViaCEP call failed", e);
        } catch (Exception e) {
            throw new ExternalCepClientException(CepProvider.VIACEP, "ViaCEP call failed", e);
        }
    }
}
