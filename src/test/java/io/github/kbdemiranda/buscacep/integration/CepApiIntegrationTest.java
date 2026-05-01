package io.github.kbdemiranda.buscacep.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kbdemiranda.buscacep.client.ViaCepClient;
import io.github.kbdemiranda.buscacep.client.WiremockCepClient;
import io.github.kbdemiranda.buscacep.dto.CepResponseDTO;
import io.github.kbdemiranda.buscacep.model.CepProvider;
import io.github.kbdemiranda.buscacep.model.CepQueryStatus;
import io.github.kbdemiranda.buscacep.repository.CepQueryLogRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:buscacep_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.url=jdbc:h2:mem:buscacep_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
    "spring.flyway.user=sa",
    "spring.flyway.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CepApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CepQueryLogRepository cepQueryLogRepository;

    @MockitoBean
    private WiremockCepClient wiremockCepClient;

    @MockitoBean
    private ViaCepClient viaCepClient;

    @BeforeEach
    void setUp() {
        cepQueryLogRepository.deleteAll();
    }

    @Test
    void shouldReturn200AndPersistWiremockSuccessWhenCepIsFoundInWiremock() throws Exception {
        when(wiremockCepClient.findByCep("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));

        mockMvc.perform(get("/api/v1/ceps/{cep}", "04364030"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cep").value("04364-030"))
            .andExpect(jsonPath("$.logradouro").value("Rua de teste"))
            .andExpect(jsonPath("$.bairro").value("Cidade Nova"))
            .andExpect(jsonPath("$.localidade").value("Sao Paulo"))
            .andExpect(jsonPath("$.uf").value("SP"));

        assertThat(cepQueryLogRepository.count()).isEqualTo(1);
        var log = cepQueryLogRepository.findAll().getFirst();
        assertThat(log.getProvider()).isEqualTo(CepProvider.WIREMOCK);
        assertThat(log.getStatus()).isEqualTo(CepQueryStatus.SUCCESS);
        assertThat(log.getCep()).isEqualTo("04364030");
        verify(viaCepClient, never()).findByCep("04364030");
    }

    @Test
    void shouldFallbackToViaCepWhenWiremockDoesNotFindCep() throws Exception {
        when(wiremockCepClient.findByCep("30140071")).thenReturn(Optional.empty());
        when(viaCepClient.findByCep("30140071")).thenReturn(Optional.of(buildResponse("30140-071")));

        mockMvc.perform(get("/api/v1/ceps/{cep}", "30140071"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cep").value("30140-071"));

        var inOrder = inOrder(wiremockCepClient, viaCepClient);
        inOrder.verify(wiremockCepClient).findByCep("30140071");
        inOrder.verify(viaCepClient).findByCep("30140071");

        assertThat(cepQueryLogRepository.count()).isEqualTo(1);
        var log = cepQueryLogRepository.findAll().getFirst();
        assertThat(log.getProvider()).isEqualTo(CepProvider.VIACEP);
        assertThat(log.getStatus()).isEqualTo(CepQueryStatus.SUCCESS);
    }

    @Test
    void shouldReturn404AndPersistNotFoundWhenCepDoesNotExist() throws Exception {
        when(wiremockCepClient.findByCep("00000000")).thenReturn(Optional.empty());
        when(viaCepClient.findByCep("00000000")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/ceps/{cep}", "00000000"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("CEP não encontrado"));

        assertThat(cepQueryLogRepository.count()).isEqualTo(1);
        var log = cepQueryLogRepository.findAll().getFirst();
        assertThat(log.getStatus()).isEqualTo(CepQueryStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn400AndNotCallClientsForInvalidCep() throws Exception {
        mockMvc.perform(get("/api/v1/ceps/{cep}", "123"))
            .andExpect(status().isBadRequest());

        verify(wiremockCepClient, never()).findByCep(org.mockito.ArgumentMatchers.anyString());
        verify(viaCepClient, never()).findByCep(org.mockito.ArgumentMatchers.anyString());
        assertThat(cepQueryLogRepository.count()).isZero();
    }

    @Test
    void shouldNormalizeFormattedCepBeforePersisting() throws Exception {
        when(wiremockCepClient.findByCep("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));

        mockMvc.perform(get("/api/v1/ceps/{cep}", "04364-030"))
            .andExpect(status().isOk());

        assertThat(cepQueryLogRepository.count()).isEqualTo(1);
        var log = cepQueryLogRepository.findAll().getFirst();
        assertThat(log.getCep()).isEqualTo("04364030");
    }

    @Test
    void shouldReturnPaginationForCustomPageAndSize() throws Exception {
        when(wiremockCepClient.findByCep("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        performLookup("04364030");

        mockMvc.perform(get("/api/v1/cep-consultas?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldUseDefaultPagination() throws Exception {
        when(wiremockCepClient.findByCep("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        performLookup("04364030");

        mockMvc.perform(get("/api/v1/cep-consultas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnPageOneWithSizeFive() throws Exception {
        when(wiremockCepClient.findByCep("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        when(wiremockCepClient.findByCep("30140071")).thenReturn(Optional.empty());
        when(viaCepClient.findByCep("30140071")).thenReturn(Optional.of(buildResponse("30140-071")));
        when(wiremockCepClient.findByCep("01001000")).thenReturn(Optional.of(buildResponse("01001-000")));
        when(wiremockCepClient.findByCep("04661200")).thenReturn(Optional.of(buildResponse("04661-200")));
        when(wiremockCepClient.findByCep("04730090")).thenReturn(Optional.of(buildResponse("04730-090")));
        when(wiremockCepClient.findByCep("01153000")).thenReturn(Optional.of(buildResponse("01153-000")));

        performLookup("04364030");
        performLookup("30140071");
        performLookup("01001000");
        performLookup("04661200");
        performLookup("04730090");
        performLookup("01153000");

        mockMvc.perform(get("/api/v1/cep-consultas?page=1&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(6))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldFilterHistoryByCep() throws Exception {
        when(wiremockCepClient.findByCep("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        when(wiremockCepClient.findByCep("01001000")).thenReturn(Optional.of(buildResponse("01001-000")));
        performLookup("04364030");
        performLookup("01001000");

        mockMvc.perform(get("/api/v1/cep-consultas?cep=04364-030"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].cep").value("04364030"));
    }

    @Test
    void shouldFilterHistoryByStatus() throws Exception {
        when(wiremockCepClient.findByCep("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        when(wiremockCepClient.findByCep("00000000")).thenReturn(Optional.empty());
        when(viaCepClient.findByCep("00000000")).thenReturn(Optional.empty());
        performLookup("04364030");
        mockMvc.perform(get("/api/v1/ceps/{cep}", "00000000"))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/cep-consultas?status=NOT_FOUND"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].status").value("NOT_FOUND"));
    }

    @Test
    void shouldFilterHistoryByProvider() throws Exception {
        when(wiremockCepClient.findByCep("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        when(wiremockCepClient.findByCep("30140071")).thenReturn(Optional.empty());
        when(viaCepClient.findByCep("30140071")).thenReturn(Optional.of(buildResponse("30140-071")));
        performLookup("04364030");
        performLookup("30140071");

        mockMvc.perform(get("/api/v1/cep-consultas?provider=VIACEP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].provider").value("VIACEP"));
    }

    @Test
    void shouldFilterHistoryByDateRange() throws Exception {
        when(wiremockCepClient.findByCep("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        performLookup("04364030");

        mockMvc.perform(get("/api/v1/cep-consultas")
                .param("dateFrom", "2000-01-01T00:00:00")
                .param("dateTo", "2100-01-01T00:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/cep-consultas")
                .param("dateFrom", "2100-01-01T00:00:00")
                .param("dateTo", "2100-01-02T00:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldFilterHistoryUsingCombinedFilters() throws Exception {
        when(wiremockCepClient.findByCep("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        when(wiremockCepClient.findByCep("30140071")).thenReturn(Optional.empty());
        when(viaCepClient.findByCep("30140071")).thenReturn(Optional.of(buildResponse("30140-071")));
        performLookup("04364030");
        performLookup("30140071");

        mockMvc.perform(get("/api/v1/cep-consultas")
                .param("cep", "30140-071")
                .param("status", "SUCCESS")
                .param("provider", "VIACEP")
                .param("dateFrom", "2000-01-01T00:00:00")
                .param("dateTo", "2100-01-01T00:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].cep").value("30140071"))
            .andExpect(jsonPath("$.content[0].provider").value("VIACEP"))
            .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));
    }

    @Test
    void shouldReturn400WhenDateRangeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/cep-consultas")
                .param("dateFrom", "2026-04-30T23:59:59")
                .param("dateTo", "2026-04-01T00:00:00"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("dateFrom must be before or equal to dateTo"));
    }

    @Test
    void shouldReturn400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/cep-consultas").param("status", "DONE"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenDateIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/cep-consultas").param("dateFrom", "2026-04-01"))
            .andExpect(status().isBadRequest());
    }

    private void performLookup(String cep) throws Exception {
        mockMvc.perform(get("/api/v1/ceps/{cep}", cep))
            .andExpect(status().isOk());
    }

    private static CepResponseDTO buildResponse(String cep) {
        return CepResponseDTO.builder()
            .cep(cep)
            .logradouro("Rua de teste")
            .bairro("Cidade Nova")
            .localidade("Sao Paulo")
            .uf("SP")
            .build();
    }
}
