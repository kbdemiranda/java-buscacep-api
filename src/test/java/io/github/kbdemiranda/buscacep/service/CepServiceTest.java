package io.github.kbdemiranda.buscacep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kbdemiranda.buscacep.client.ViaCepClient;
import io.github.kbdemiranda.buscacep.client.WiremockCepClient;
import io.github.kbdemiranda.buscacep.dto.CepResponseDTO;
import io.github.kbdemiranda.buscacep.exception.CepNotFoundException;
import io.github.kbdemiranda.buscacep.exception.CepQueryLogNotFoundException;
import io.github.kbdemiranda.buscacep.exception.ExternalCepClientException;
import io.github.kbdemiranda.buscacep.exception.InvalidCepException;
import io.github.kbdemiranda.buscacep.model.CepProvider;
import io.github.kbdemiranda.buscacep.model.CepQueryLog;
import io.github.kbdemiranda.buscacep.model.CepQueryStatus;
import io.github.kbdemiranda.buscacep.repository.CepQueryLogRepository;
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
class CepServiceTest {

    @Mock
    private WiremockCepClient wiremockCepClient;

    @Mock
    private ViaCepClient viaCepClient;

    @Mock
    private CepQueryLogRepository cepQueryLogRepository;

    private CepService cepService;

    @BeforeEach
    void setUp() {
        cepService = new CepService(wiremockCepClient, viaCepClient, cepQueryLogRepository, new ObjectMapper());
    }

    @Test
    void shouldReturnWiremockResultAndLogSuccessWhenCepIsFoundInWiremock() {
        CepResponseDTO response = buildResponse("04364-030");
        when(wiremockCepClient.findByCep("04364030")).thenReturn(Optional.of(response));

        CepResponseDTO result = cepService.findCep("04364030");

        assertThat(result).isEqualTo(response);
        verify(viaCepClient, never()).findByCep(any());

        ArgumentCaptor<CepQueryLog> logCaptor = ArgumentCaptor.forClass(CepQueryLog.class);
        verify(cepQueryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProvider()).isEqualTo(CepProvider.WIREMOCK);
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(CepQueryStatus.SUCCESS);
    }

    @Test
    void shouldUseViaCepAndLogSuccessWhenWiremockDoesNotFindCep() {
        CepResponseDTO response = buildResponse("01001-000");
        when(wiremockCepClient.findByCep("01001000")).thenReturn(Optional.empty());
        when(viaCepClient.findByCep("01001000")).thenReturn(Optional.of(response));

        CepResponseDTO result = cepService.findCep("01001000");

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<CepQueryLog> logCaptor = ArgumentCaptor.forClass(CepQueryLog.class);
        verify(cepQueryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProvider()).isEqualTo(CepProvider.VIACEP);
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(CepQueryStatus.SUCCESS);
    }

    @Test
    void shouldThrowNotFoundAndLogNotFoundWhenCepIsMissingInBothProviders() {
        when(wiremockCepClient.findByCep("00000000")).thenReturn(Optional.empty());
        when(viaCepClient.findByCep("00000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cepService.findCep("00000000"))
            .isInstanceOf(CepNotFoundException.class)
            .hasMessage("CEP não encontrado");

        ArgumentCaptor<CepQueryLog> logCaptor = ArgumentCaptor.forClass(CepQueryLog.class);
        verify(cepQueryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProvider()).isEqualTo(CepProvider.VIACEP);
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(CepQueryStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectInvalidCepWithLessThanEightDigitsAndNotCallClients() {
        assertThatThrownBy(() -> cepService.findCep("123"))
            .isInstanceOf(InvalidCepException.class);

        verifyNoInteractions(wiremockCepClient, viaCepClient, cepQueryLogRepository);
    }

    @Test
    void shouldRejectInvalidCepWithLettersAndNotCallClients() {
        assertThatThrownBy(() -> cepService.findCep("12AB5678"))
            .isInstanceOf(InvalidCepException.class);

        verifyNoInteractions(wiremockCepClient, viaCepClient, cepQueryLogRepository);
    }

    @Test
    void shouldLogErrorAndRethrowWhenWiremockFails() {
        ExternalCepClientException error =
            new ExternalCepClientException(CepProvider.WIREMOCK, "WireMock timeout", new RuntimeException("timeout"));
        when(wiremockCepClient.findByCep("04364030")).thenThrow(error);

        assertThatThrownBy(() -> cepService.findCep("04364030"))
            .isInstanceOf(ExternalCepClientException.class)
            .hasMessage("WireMock timeout");

        ArgumentCaptor<CepQueryLog> logCaptor = ArgumentCaptor.forClass(CepQueryLog.class);
        verify(cepQueryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(CepQueryStatus.ERROR);
        assertThat(logCaptor.getValue().getProvider()).isEqualTo(CepProvider.WIREMOCK);
        assertThat(logCaptor.getValue().getErrorMessage()).isEqualTo("WireMock timeout");
    }

    @Test
    void shouldLogErrorAndRethrowWhenViaCepFails() {
        ExternalCepClientException error =
            new ExternalCepClientException(CepProvider.VIACEP, "ViaCEP unavailable", new RuntimeException("down"));
        when(wiremockCepClient.findByCep("30140071")).thenReturn(Optional.empty());
        when(viaCepClient.findByCep("30140071")).thenThrow(error);

        assertThatThrownBy(() -> cepService.findCep("30140071"))
            .isInstanceOf(ExternalCepClientException.class)
            .hasMessage("ViaCEP unavailable");

        ArgumentCaptor<CepQueryLog> logCaptor = ArgumentCaptor.forClass(CepQueryLog.class);
        verify(cepQueryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(CepQueryStatus.ERROR);
        assertThat(logCaptor.getValue().getProvider()).isEqualTo(CepProvider.VIACEP);
        assertThat(logCaptor.getValue().getErrorMessage()).isEqualTo("ViaCEP unavailable");
    }

    @Test
    void shouldReturnDetailWhenExternalIdExists() {
        UUID externalId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        CepQueryLog log = CepQueryLog.builder()
            .externalId(externalId)
            .cep("04364030")
            .provider(CepProvider.WIREMOCK)
            .status(CepQueryStatus.SUCCESS)
            .requestTimestamp(now)
            .createdAt(now)
            .updatedAt(now)
            .build();
        when(cepQueryLogRepository.findByExternalId(externalId)).thenReturn(Optional.of(log));

        var result = cepService.findByExternalId(externalId);

        assertThat(result.externalId()).isEqualTo(externalId);
        assertThat(result.cep()).isEqualTo("04364030");
        assertThat(result.provider()).isEqualTo("WIREMOCK");
        assertThat(result.status()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldThrowNotFoundWhenExternalIdDoesNotExist() {
        UUID externalId = UUID.randomUUID();
        when(cepQueryLogRepository.findByExternalId(externalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cepService.findByExternalId(externalId))
            .isInstanceOf(CepQueryLogNotFoundException.class);
    }

    private static CepResponseDTO buildResponse(String cep) {
        return CepResponseDTO.builder()
            .cep(cep)
            .logradouro("Rua de teste")
            .localidade("Sao Paulo")
            .uf("SP")
            .build();
    }
}
