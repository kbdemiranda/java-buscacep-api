package io.github.kbdemiranda.zipcode.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kbdemiranda.zipcode.search.client.ViaCepZipCodeClient;
import io.github.kbdemiranda.zipcode.search.client.WireMockZipCodeClient;
import io.github.kbdemiranda.zipcode.search.dto.ZipCodeResponseDTO;
import io.github.kbdemiranda.zipcode.search.exception.ZipCodeNotFoundException;
import io.github.kbdemiranda.zipcode.search.exception.ZipCodeQueryLogNotFoundException;
import io.github.kbdemiranda.zipcode.search.exception.ExternalZipCodeClientException;
import io.github.kbdemiranda.zipcode.search.exception.InvalidZipCodeException;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeProvider;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeQueryLog;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeQueryStatus;
import io.github.kbdemiranda.zipcode.search.repository.ZipCodeQueryLogRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ZipCodeServiceTest {

    @Mock
    private WireMockZipCodeClient wireMockZipCodeClient;

    @Mock
    private ViaCepZipCodeClient viaCepZipCodeClient;

    @Mock
    private ZipCodeQueryLogRepository zipCodeQueryLogRepository;

    private ZipCodeService zipCodeService;

    @BeforeEach
    void setUp() {
        zipCodeService = new ZipCodeService(wireMockZipCodeClient, viaCepZipCodeClient, zipCodeQueryLogRepository, new ObjectMapper());
    }

    @Test
    void shouldReturnWireMockResultAndLogSuccessWhenZipCodeIsFoundInWireMock() {
        ZipCodeResponseDTO response = buildResponse("04364-030");
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(response));

        ZipCodeResponseDTO result = zipCodeService.searchZipCode("04364030");

        assertThat(result).isEqualTo(response);
        verify(viaCepZipCodeClient, never()).searchZipCode(any());

        ArgumentCaptor<ZipCodeQueryLog> logCaptor = ArgumentCaptor.forClass(ZipCodeQueryLog.class);
        verify(zipCodeQueryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProvider()).isEqualTo(ZipCodeProvider.WIREMOCK);
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(ZipCodeQueryStatus.SUCCESS);
    }

    @Test
    void shouldUseViaCepAndLogSuccessWhenWireMockDoesNotFindZipCode() {
        ZipCodeResponseDTO response = buildResponse("01001-000");
        when(wireMockZipCodeClient.searchZipCode("01001000")).thenReturn(Optional.empty());
        when(viaCepZipCodeClient.searchZipCode("01001000")).thenReturn(Optional.of(response));

        ZipCodeResponseDTO result = zipCodeService.searchZipCode("01001000");

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<ZipCodeQueryLog> logCaptor = ArgumentCaptor.forClass(ZipCodeQueryLog.class);
        verify(zipCodeQueryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProvider()).isEqualTo(ZipCodeProvider.VIACEP);
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(ZipCodeQueryStatus.SUCCESS);
    }

    @Test
    void shouldThrowNotFoundAndLogNotFoundWhenZipCodeIsMissingInBothProviders() {
        when(wireMockZipCodeClient.searchZipCode("00000000")).thenReturn(Optional.empty());
        when(viaCepZipCodeClient.searchZipCode("00000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zipCodeService.searchZipCode("00000000"))
            .isInstanceOf(ZipCodeNotFoundException.class)
            .hasMessage("CEP não encontrado");

        ArgumentCaptor<ZipCodeQueryLog> logCaptor = ArgumentCaptor.forClass(ZipCodeQueryLog.class);
        verify(zipCodeQueryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProvider()).isEqualTo(ZipCodeProvider.VIACEP);
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(ZipCodeQueryStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectInvalidZipCodeWithLessThanEightDigitsAndNotCallClients() {
        assertThatThrownBy(() -> zipCodeService.searchZipCode("123"))
            .isInstanceOf(InvalidZipCodeException.class);

        verifyNoInteractions(wireMockZipCodeClient, viaCepZipCodeClient, zipCodeQueryLogRepository);
    }

    @Test
    void shouldRejectInvalidZipCodeWithLettersAndNotCallClients() {
        assertThatThrownBy(() -> zipCodeService.searchZipCode("12AB5678"))
            .isInstanceOf(InvalidZipCodeException.class);

        verifyNoInteractions(wireMockZipCodeClient, viaCepZipCodeClient, zipCodeQueryLogRepository);
    }

    @Test
    void shouldLogErrorAndRethrowWhenWiremockFails() {
        ExternalZipCodeClientException error =
            new ExternalZipCodeClientException(ZipCodeProvider.WIREMOCK, "WireMock timeout", new RuntimeException("timeout"));
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenThrow(error);

        assertThatThrownBy(() -> zipCodeService.searchZipCode("04364030"))
            .isInstanceOf(ExternalZipCodeClientException.class)
            .hasMessage("WireMock timeout");

        ArgumentCaptor<ZipCodeQueryLog> logCaptor = ArgumentCaptor.forClass(ZipCodeQueryLog.class);
        verify(zipCodeQueryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(ZipCodeQueryStatus.ERROR);
        assertThat(logCaptor.getValue().getProvider()).isEqualTo(ZipCodeProvider.WIREMOCK);
        assertThat(logCaptor.getValue().getErrorMessage()).isEqualTo("WireMock timeout");
    }

    @Test
    void shouldLogErrorAndRethrowWhenViaCepFails() {
        ExternalZipCodeClientException error =
            new ExternalZipCodeClientException(ZipCodeProvider.VIACEP, "ViaCEP unavailable", new RuntimeException("down"));
        when(wireMockZipCodeClient.searchZipCode("30140071")).thenReturn(Optional.empty());
        when(viaCepZipCodeClient.searchZipCode("30140071")).thenThrow(error);

        assertThatThrownBy(() -> zipCodeService.searchZipCode("30140071"))
            .isInstanceOf(ExternalZipCodeClientException.class)
            .hasMessage("ViaCEP unavailable");

        ArgumentCaptor<ZipCodeQueryLog> logCaptor = ArgumentCaptor.forClass(ZipCodeQueryLog.class);
        verify(zipCodeQueryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(ZipCodeQueryStatus.ERROR);
        assertThat(logCaptor.getValue().getProvider()).isEqualTo(ZipCodeProvider.VIACEP);
        assertThat(logCaptor.getValue().getErrorMessage()).isEqualTo("ViaCEP unavailable");
    }

    @Test
    void shouldReturnDetailWhenExternalIdExists() {
        UUID externalId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ZipCodeQueryLog log = ZipCodeQueryLog.builder()
            .externalId(externalId)
            .zipCode("04364030")
            .provider(ZipCodeProvider.WIREMOCK)
            .status(ZipCodeQueryStatus.SUCCESS)
            .requestTimestamp(now)
            .createdAt(now)
            .updatedAt(now)
            .build();
        when(zipCodeQueryLogRepository.findByExternalId(externalId)).thenReturn(Optional.of(log));

        var result = zipCodeService.findByExternalId(externalId);

        assertThat(result.externalId()).isEqualTo(externalId);
        assertThat(result.zipCode()).isEqualTo("04364030");
        assertThat(result.provider()).isEqualTo("WIREMOCK");
        assertThat(result.status()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldThrowNotFoundWhenExternalIdDoesNotExist() {
        UUID externalId = UUID.randomUUID();
        when(zipCodeQueryLogRepository.findByExternalId(externalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zipCodeService.findByExternalId(externalId))
            .isInstanceOf(ZipCodeQueryLogNotFoundException.class);
    }

    private static ZipCodeResponseDTO buildResponse(String cep) {
        return ZipCodeResponseDTO.builder()
            .zipCode(cep)
            .logradouro("Rua de teste")
            .localidade("Sao Paulo")
            .uf("SP")
            .build();
    }
}
