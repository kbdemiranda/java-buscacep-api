package io.github.kbdemiranda.zipcode.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kbdemiranda.zipcode.search.client.ViaCepClient;
import io.github.kbdemiranda.zipcode.search.client.WiremockCepClient;
import io.github.kbdemiranda.zipcode.search.dto.CepQueryLogFilterDTO;
import io.github.kbdemiranda.zipcode.search.dto.CepQueryLogDetailResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.CepQueryLogResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.CepResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.PageResponse;
import io.github.kbdemiranda.zipcode.search.exception.CepNotFoundException;
import io.github.kbdemiranda.zipcode.search.exception.CepQueryLogNotFoundException;
import io.github.kbdemiranda.zipcode.search.exception.ExternalCepClientException;
import io.github.kbdemiranda.zipcode.search.exception.InvalidCepException;
import io.github.kbdemiranda.zipcode.search.model.CepProvider;
import io.github.kbdemiranda.zipcode.search.model.CepQueryLog;
import io.github.kbdemiranda.zipcode.search.model.CepQueryStatus;
import io.github.kbdemiranda.zipcode.search.repository.CepQueryLogRepository;
import io.github.kbdemiranda.zipcode.search.repository.specification.CepQueryLogSpecification;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public PageResponse<CepQueryLogResponseDTO> findAll(int page, int size, CepQueryLogFilterDTO filter) {
        validateDateRange(filter);
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestTimestamp").descending());
        var spec = CepQueryLogSpecification.withFilters(filter);
        var pageResult = cepQueryLogRepository.findAll(spec, pageable).map(this::toResponseDTO);
        return new PageResponse<>(
            pageResult.getContent(),
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages()
        );
    }

    public CepQueryLogDetailResponseDTO findByExternalId(UUID externalId) {
        CepQueryLog log = cepQueryLogRepository.findByExternalId(externalId)
            .orElseThrow(() -> new CepQueryLogNotFoundException(externalId));
        return toDetailResponseDTO(log);
    }

    private String normalizeAndValidateCep(String rawCep) {
        String normalized = rawCep.replaceAll("\\D", "").trim();
        if (!normalized.matches("\\d{8}")) {
            throw new InvalidCepException(rawCep);
        }
        return normalized;
    }

    private void validateDateRange(CepQueryLogFilterDTO filter) {
        if (filter.getDateFrom() != null && filter.getDateTo() != null && filter.getDateFrom().isAfter(filter.getDateTo())) {
            throw new IllegalArgumentException("dateFrom must be before or equal to dateTo");
        }
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

    private CepQueryLogResponseDTO toResponseDTO(CepQueryLog log) {
        return CepQueryLogResponseDTO.builder()
            .externalId(log.getExternalId())
            .cep(log.getCep())
            .provider(log.getProvider().name())
            .status(log.getStatus().name())
            .requestTimestamp(log.getRequestTimestamp())
            .build();
    }

    private CepQueryLogDetailResponseDTO toDetailResponseDTO(CepQueryLog log) {
        return CepQueryLogDetailResponseDTO.builder()
            .externalId(log.getExternalId())
            .cep(log.getCep())
            .provider(log.getProvider().name())
            .status(log.getStatus().name())
            .requestTimestamp(log.getRequestTimestamp())
            .responseBody(log.getResponseBody())
            .errorMessage(log.getErrorMessage())
            .createdAt(log.getCreatedAt())
            .updatedAt(log.getUpdatedAt())
            .build();
    }
}
