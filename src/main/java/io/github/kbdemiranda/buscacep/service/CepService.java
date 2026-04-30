package io.github.kbdemiranda.buscacep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kbdemiranda.buscacep.client.ViaCepClient;
import io.github.kbdemiranda.buscacep.client.WiremockCepClient;
import io.github.kbdemiranda.buscacep.dto.CepResponseDTO;
import io.github.kbdemiranda.buscacep.exception.CepNotFoundException;
import io.github.kbdemiranda.buscacep.exception.ExternalCepClientException;
import io.github.kbdemiranda.buscacep.exception.InvalidCepException;
import io.github.kbdemiranda.buscacep.model.CepProvider;
import io.github.kbdemiranda.buscacep.model.CepQueryLog;
import io.github.kbdemiranda.buscacep.model.CepQueryStatus;
import io.github.kbdemiranda.buscacep.repository.CepQueryLogRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CepService {

    private final WiremockCepClient wiremockCepClient;
    private final ViaCepClient viaCepClient;
    private final CepQueryLogRepository cepQueryLogRepository;
    private final ObjectMapper objectMapper;

    public CepResponseDTO findCep(String cep) {
        String normalizedCep = normalizeAndValidateCep(cep);
        LocalDateTime requestTimestamp = LocalDateTime.now();

        try {
            Optional<CepResponseDTO> wiremockResult = wiremockCepClient.findByCep(normalizedCep);
            if (wiremockResult.isPresent()) {
                CepResponseDTO response = wiremockResult.get();
                saveLog(normalizedCep, CepProvider.WIREMOCK, CepQueryStatus.SUCCESS, requestTimestamp, response, null);
                return response;
            }

            Optional<CepResponseDTO> viaCepResult = viaCepClient.findByCep(normalizedCep);
            if (viaCepResult.isPresent()) {
                CepResponseDTO response = viaCepResult.get();
                saveLog(normalizedCep, CepProvider.VIACEP, CepQueryStatus.SUCCESS, requestTimestamp, response, null);
                return response;
            }

            saveLog(normalizedCep, CepProvider.VIACEP, CepQueryStatus.NOT_FOUND, requestTimestamp, null, null);
            throw new CepNotFoundException(normalizedCep);
        } catch (ExternalCepClientException e) {
            saveLog(normalizedCep, e.getProvider(), CepQueryStatus.ERROR, requestTimestamp, null, e.getMessage());
            throw e;
        }
    }

    private String normalizeAndValidateCep(String rawCep) {
        String normalized = rawCep.replace("-", "").trim();
        if (!normalized.matches("\\d{8}")) {
            throw new InvalidCepException(rawCep);
        }
        return normalized;
    }

    private void saveLog(
        String cep,
        CepProvider provider,
        CepQueryStatus status,
        LocalDateTime requestTimestamp,
        CepResponseDTO response,
        String errorMessage
    ) {
        CepQueryLog log = CepQueryLog.builder()
            .cep(cep)
            .provider(provider)
            .status(status)
            .requestTimestamp(requestTimestamp)
            .responseBody(serializeResponse(response))
            .errorMessage(errorMessage)
            .build();
        cepQueryLogRepository.save(log);
    }

    private JsonNode serializeResponse(CepResponseDTO response) {
        if (response == null) {
            return null;
        }
        return objectMapper.valueToTree(response);
    }
}
