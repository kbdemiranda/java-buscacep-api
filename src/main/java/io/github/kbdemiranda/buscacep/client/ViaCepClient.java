package io.github.kbdemiranda.buscacep.client;

import io.github.kbdemiranda.buscacep.dto.CepResponseDTO;
import io.github.kbdemiranda.buscacep.exception.ExternalCepClientException;
import io.github.kbdemiranda.buscacep.config.CepClientProperties;
import io.github.kbdemiranda.buscacep.model.CepProvider;
import java.util.Map;
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
    @SuppressWarnings("unchecked")
    public Optional<CepResponseDTO> findByCep(String cep) {
        String url = cepClientProperties.viacepUrl() + "/{cep}/json";
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class, cep);
            if (response == null) {
                return Optional.empty();
            }

            Object erro = response.get("erro");
            if (erro instanceof Boolean && (Boolean) erro) {
                return Optional.empty();
            }

            CepResponseDTO dto = CepResponseDTO.builder()
                .cep((String) response.get("cep"))
                .logradouro((String) response.get("logradouro"))
                .complemento((String) response.get("complemento"))
                .bairro((String) response.get("bairro"))
                .localidade((String) response.get("localidade"))
                .uf((String) response.get("uf"))
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
