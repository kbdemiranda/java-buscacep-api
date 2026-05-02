package io.github.kbdemiranda.zipcode.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kbdemiranda.zipcode.search.client.ViaCepZipCodeClient;
import io.github.kbdemiranda.zipcode.search.client.WireMockZipCodeClient;
import io.github.kbdemiranda.zipcode.search.dto.ZipCodeQueryLogFilterDTO;
import io.github.kbdemiranda.zipcode.search.dto.ZipCodeQueryLogDetailResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.ZipCodeQueryLogResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.ZipCodeResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.PageResponse;
import io.github.kbdemiranda.zipcode.search.exception.ZipCodeNotFoundException;
import io.github.kbdemiranda.zipcode.search.exception.ZipCodeQueryLogNotFoundException;
import io.github.kbdemiranda.zipcode.search.exception.ExternalZipCodeClientException;
import io.github.kbdemiranda.zipcode.search.exception.InvalidZipCodeException;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeProvider;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeQueryLog;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeQueryStatus;
import io.github.kbdemiranda.zipcode.search.repository.ZipCodeQueryLogRepository;
import io.github.kbdemiranda.zipcode.search.repository.specification.ZipCodeQueryLogSpecification;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ZipCodeService {

    private final WireMockZipCodeClient wireMockZipCodeClient;
    private final ViaCepZipCodeClient viaCepZipCodeClient;
    private final ZipCodeQueryLogRepository zipCodeQueryLogRepository;
    private final ObjectMapper objectMapper;

    public ZipCodeResponseDTO searchZipCode(String zipCodeInput) {
        String normalizedZipCode = normalizeAndValidateZipCode(zipCodeInput);
        LocalDateTime requestTimestamp = LocalDateTime.now();

        try {
            Optional<ZipCodeResponseDTO> wireMockResult = wireMockZipCodeClient.searchZipCode(normalizedZipCode);
            if (wireMockResult.isPresent()) {
                ZipCodeResponseDTO response = wireMockResult.get();
                saveLog(normalizedZipCode, ZipCodeProvider.WIREMOCK, ZipCodeQueryStatus.SUCCESS, requestTimestamp, response, null);
                return response;
            }

            Optional<ZipCodeResponseDTO> viaCepResult = viaCepZipCodeClient.searchZipCode(normalizedZipCode);
            if (viaCepResult.isPresent()) {
                ZipCodeResponseDTO response = viaCepResult.get();
                saveLog(normalizedZipCode, ZipCodeProvider.VIACEP, ZipCodeQueryStatus.SUCCESS, requestTimestamp, response, null);
                return response;
            }

            saveLog(normalizedZipCode, ZipCodeProvider.VIACEP, ZipCodeQueryStatus.NOT_FOUND, requestTimestamp, null, null);
            throw new ZipCodeNotFoundException(normalizedZipCode);
        } catch (ExternalZipCodeClientException e) {
            saveLog(normalizedZipCode, e.getProvider(), ZipCodeQueryStatus.ERROR, requestTimestamp, null, e.getMessage());
            throw e;
        }
    }

    public PageResponse<ZipCodeQueryLogResponseDTO> listQueries(int page, int size, ZipCodeQueryLogFilterDTO filter) {
        validateDateRange(filter);
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestTimestamp").descending());
        var spec = ZipCodeQueryLogSpecification.withFilters(filter);
        var pageResult = zipCodeQueryLogRepository.findAll(spec, pageable).map(this::toResponseDTO);
        return new PageResponse<>(
            pageResult.getContent(),
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements(),
            pageResult.getTotalPages()
        );
    }

    public ZipCodeQueryLogDetailResponseDTO findByExternalId(UUID externalId) {
        ZipCodeQueryLog log = zipCodeQueryLogRepository.findByExternalId(externalId)
            .orElseThrow(() -> new ZipCodeQueryLogNotFoundException(externalId));
        return toDetailResponseDTO(log);
    }

    private String normalizeAndValidateZipCode(String rawZipCode) {
        String normalized = rawZipCode.replaceAll("\\D", "").trim();
        if (!normalized.matches("\\d{8}")) {
            throw new InvalidZipCodeException(rawZipCode);
        }
        return normalized;
    }

    private void validateDateRange(ZipCodeQueryLogFilterDTO filter) {
        if (filter.dateFrom() != null && filter.dateTo() != null && filter.dateFrom().isAfter(filter.dateTo())) {
            throw new IllegalArgumentException("dateFrom must be before or equal to dateTo");
        }
    }

    private void saveLog(
        String zipCode,
        ZipCodeProvider provider,
        ZipCodeQueryStatus status,
        LocalDateTime requestTimestamp,
        ZipCodeResponseDTO response,
        String errorMessage
    ) {
        ZipCodeQueryLog log = ZipCodeQueryLog.builder()
            .zipCode(zipCode)
            .provider(provider)
            .status(status)
            .requestTimestamp(requestTimestamp)
            .responseBody(serializeResponse(response))
            .errorMessage(errorMessage)
            .build();
        zipCodeQueryLogRepository.save(log);
    }

    private JsonNode serializeResponse(ZipCodeResponseDTO response) {
        if (response == null) {
            return null;
        }
        return objectMapper.valueToTree(response);
    }

    private ZipCodeQueryLogResponseDTO toResponseDTO(ZipCodeQueryLog log) {
        return ZipCodeQueryLogResponseDTO.builder()
            .externalId(log.getExternalId())
            .zipCode(log.getZipCode())
            .provider(log.getProvider().name())
            .status(log.getStatus().name())
            .requestTimestamp(log.getRequestTimestamp())
            .build();
    }

    private ZipCodeQueryLogDetailResponseDTO toDetailResponseDTO(ZipCodeQueryLog log) {
        return ZipCodeQueryLogDetailResponseDTO.builder()
            .externalId(log.getExternalId())
            .zipCode(log.getZipCode())
            .provider(log.getProvider().name())
            .status(log.getStatus().name())
            .requestTimestamp(log.getRequestTimestamp())
            .responseBody(deserializeResponseBody(log.getResponseBody()))
            .errorMessage(log.getErrorMessage())
            .createdAt(log.getCreatedAt())
            .updatedAt(log.getUpdatedAt())
            .build();
    }

    private Map<String, Object> deserializeResponseBody(JsonNode responseBody) {
        if (responseBody == null || responseBody.isNull()) {
            return null;
        }
        return objectMapper.convertValue(responseBody, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }
}
