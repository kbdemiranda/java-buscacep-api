package io.github.kbdemiranda.zipcode.search.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kbdemiranda.zipcode.search.client.ViaCepZipCodeClient;
import io.github.kbdemiranda.zipcode.search.client.WireMockZipCodeClient;
import io.github.kbdemiranda.zipcode.search.dto.ZipCodeResponseDTO;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeProvider;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeQueryStatus;
import io.github.kbdemiranda.zipcode.search.repository.ZipCodeQueryLogRepository;
import java.util.Optional;
import java.util.UUID;
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
class ZipCodeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ZipCodeQueryLogRepository zipCodeQueryLogRepository;

    @MockitoBean
    private WireMockZipCodeClient wireMockZipCodeClient;

    @MockitoBean
    private ViaCepZipCodeClient viaCepZipCodeClient;

    @BeforeEach
    void setUp() {
        zipCodeQueryLogRepository.deleteAll();
    }

    @Test
    void shouldReturnZipCodeWhenFoundInWireMock() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));

        mockMvc.perform(get("/api/v1/zip-codes/{cep}", "04364030"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cep").value("04364-030"))
            .andExpect(jsonPath("$.logradouro").value("Rua de teste"))
            .andExpect(jsonPath("$.bairro").value("Cidade Nova"))
            .andExpect(jsonPath("$.localidade").value("Sao Paulo"))
            .andExpect(jsonPath("$.uf").value("SP"));

        assertThat(zipCodeQueryLogRepository.count()).isEqualTo(1);
        var log = zipCodeQueryLogRepository.findAll().getFirst();
        assertThat(log.getProvider()).isEqualTo(ZipCodeProvider.WIREMOCK);
        assertThat(log.getStatus()).isEqualTo(ZipCodeQueryStatus.SUCCESS);
        assertThat(log.getZipCode()).isEqualTo("04364030");
        verify(viaCepZipCodeClient, never()).searchZipCode("04364030");
    }

    @Test
    void shouldFallbackToViaCepWhenWireMockDoesNotFindZipCode() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("30140071")).thenReturn(Optional.empty());
        when(viaCepZipCodeClient.searchZipCode("30140071")).thenReturn(Optional.of(buildResponse("30140-071")));

        mockMvc.perform(get("/api/v1/zip-codes/{cep}", "30140071"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cep").value("30140-071"));

        var inOrder = inOrder(wireMockZipCodeClient, viaCepZipCodeClient);
        inOrder.verify(wireMockZipCodeClient).searchZipCode("30140071");
        inOrder.verify(viaCepZipCodeClient).searchZipCode("30140071");

        assertThat(zipCodeQueryLogRepository.count()).isEqualTo(1);
        var log = zipCodeQueryLogRepository.findAll().getFirst();
        assertThat(log.getProvider()).isEqualTo(ZipCodeProvider.VIACEP);
        assertThat(log.getStatus()).isEqualTo(ZipCodeQueryStatus.SUCCESS);
    }

    @Test
    void shouldReturnNotFoundWhenZipCodeDoesNotExist() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("00000000")).thenReturn(Optional.empty());
        when(viaCepZipCodeClient.searchZipCode("00000000")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/zip-codes/{cep}", "00000000"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("CEP não encontrado"))
            .andExpect(jsonPath("$.path").value("/api/v1/zip-codes/00000000"));

        assertThat(zipCodeQueryLogRepository.count()).isEqualTo(1);
        var log = zipCodeQueryLogRepository.findAll().getFirst();
        assertThat(log.getStatus()).isEqualTo(ZipCodeQueryStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn400AndNotCallClientsForInvalidZipCode() throws Exception {
        mockMvc.perform(get("/api/v1/zip-codes/{cep}", "123"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/zip-codes/123"));

        verify(wireMockZipCodeClient, never()).searchZipCode(org.mockito.ArgumentMatchers.anyString());
        verify(viaCepZipCodeClient, never()).searchZipCode(org.mockito.ArgumentMatchers.anyString());
        assertThat(zipCodeQueryLogRepository.count()).isZero();
    }

    @Test
    void shouldNormalizeFormattedZipCodeBeforePersisting() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));

        mockMvc.perform(get("/api/v1/zip-codes/{cep}", "04364-030"))
            .andExpect(status().isOk());

        assertThat(zipCodeQueryLogRepository.count()).isEqualTo(1);
        var log = zipCodeQueryLogRepository.findAll().getFirst();
        assertThat(log.getZipCode()).isEqualTo("04364030");
    }

    @Test
    void shouldReturnPaginationForCustomPageAndSize() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        performLookup("04364030");

        mockMvc.perform(get("/api/v1/zip-code-queries?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldUseDefaultPagination() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        performLookup("04364030");

        mockMvc.perform(get("/api/v1/zip-code-queries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnPageOneWithSizeFive() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        when(wireMockZipCodeClient.searchZipCode("30140071")).thenReturn(Optional.empty());
        when(viaCepZipCodeClient.searchZipCode("30140071")).thenReturn(Optional.of(buildResponse("30140-071")));
        when(wireMockZipCodeClient.searchZipCode("01001000")).thenReturn(Optional.of(buildResponse("01001-000")));
        when(wireMockZipCodeClient.searchZipCode("04661200")).thenReturn(Optional.of(buildResponse("04661-200")));
        when(wireMockZipCodeClient.searchZipCode("04730090")).thenReturn(Optional.of(buildResponse("04730-090")));
        when(wireMockZipCodeClient.searchZipCode("01153000")).thenReturn(Optional.of(buildResponse("01153-000")));

        performLookup("04364030");
        performLookup("30140071");
        performLookup("01001000");
        performLookup("04661200");
        performLookup("04730090");
        performLookup("01153000");

        mockMvc.perform(get("/api/v1/zip-code-queries?page=1&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(6))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldListZipCodeQueriesWithFilters() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        when(wireMockZipCodeClient.searchZipCode("01001000")).thenReturn(Optional.of(buildResponse("01001-000")));
        performLookup("04364030");
        performLookup("01001000");

        mockMvc.perform(get("/api/v1/zip-code-queries?zipCode=04364-030"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].cep").value("04364030"));
    }

    @Test
    void shouldFilterQueryHistoryByStatus() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        when(wireMockZipCodeClient.searchZipCode("00000000")).thenReturn(Optional.empty());
        when(viaCepZipCodeClient.searchZipCode("00000000")).thenReturn(Optional.empty());
        performLookup("04364030");
        mockMvc.perform(get("/api/v1/zip-codes/{cep}", "00000000"))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/zip-code-queries?status=NOT_FOUND"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].status").value("NOT_FOUND"));
    }

    @Test
    void shouldFilterQueryHistoryByProvider() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        when(wireMockZipCodeClient.searchZipCode("30140071")).thenReturn(Optional.empty());
        when(viaCepZipCodeClient.searchZipCode("30140071")).thenReturn(Optional.of(buildResponse("30140-071")));
        performLookup("04364030");
        performLookup("30140071");

        mockMvc.perform(get("/api/v1/zip-code-queries?provider=VIACEP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].provider").value("VIACEP"));
    }

    @Test
    void shouldFilterQueryHistoryByDateRange() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        performLookup("04364030");

        mockMvc.perform(get("/api/v1/zip-code-queries")
                .param("dateFrom", "2000-01-01T00:00:00")
                .param("dateTo", "2100-01-01T00:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/zip-code-queries")
                .param("dateFrom", "2100-01-01T00:00:00")
                .param("dateTo", "2100-01-02T00:00:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldFilterQueryHistoryUsingCombinedFilters() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        when(wireMockZipCodeClient.searchZipCode("30140071")).thenReturn(Optional.empty());
        when(viaCepZipCodeClient.searchZipCode("30140071")).thenReturn(Optional.of(buildResponse("30140-071")));
        performLookup("04364030");
        performLookup("30140071");

        mockMvc.perform(get("/api/v1/zip-code-queries")
                .param("zipCode", "30140-071")
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
        mockMvc.perform(get("/api/v1/zip-code-queries")
                .param("dateFrom", "2026-04-30T23:59:59")
                .param("dateTo", "2026-04-01T00:00:00"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("dateFrom must be before or equal to dateTo"))
            .andExpect(jsonPath("$.path").value("/api/v1/zip-code-queries"));
    }

    @Test
    void shouldReturn400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/zip-code-queries").param("status", "DONE"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Parâmetro inválido"))
            .andExpect(jsonPath("$.path").value("/api/v1/zip-code-queries"));
    }

    @Test
    void shouldReturn400WhenProviderIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/zip-code-queries").param("provider", "ABC"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Parâmetro inválido"))
            .andExpect(jsonPath("$.path").value("/api/v1/zip-code-queries"));
    }

    @Test
    void shouldReturn400WhenDateIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/zip-code-queries").param("dateFrom", "2026-04-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Parâmetro inválido"))
            .andExpect(jsonPath("$.path").value("/api/v1/zip-code-queries"));
    }

    @Test
    void shouldReturnZipCodeFieldsInDetailResponseBody() throws Exception {
        when(wireMockZipCodeClient.searchZipCode("04364030")).thenReturn(Optional.of(buildResponse("04364-030")));
        performLookup("04364030");

        UUID externalId = zipCodeQueryLogRepository.findAll().getFirst().getExternalId();

        mockMvc.perform(get("/api/v1/zip-code-queries/{externalId}", externalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.responseBody.cep").value("04364-030"))
            .andExpect(jsonPath("$.responseBody.logradouro").value("Rua de teste"))
            .andExpect(jsonPath("$.responseBody.bairro").value("Cidade Nova"))
            .andExpect(jsonPath("$.responseBody.localidade").value("Sao Paulo"))
            .andExpect(jsonPath("$.responseBody.uf").value("SP"))
            .andExpect(jsonPath("$.responseBody.object").doesNotExist())
            .andExpect(jsonPath("$.responseBody.containerNode").doesNotExist());
    }

    private void performLookup(String cep) throws Exception {
        mockMvc.perform(get("/api/v1/zip-codes/{cep}", cep))
            .andExpect(status().isOk());
    }

    private static ZipCodeResponseDTO buildResponse(String cep) {
        return ZipCodeResponseDTO.builder()
            .zipCode(cep)
            .logradouro("Rua de teste")
            .bairro("Cidade Nova")
            .localidade("Sao Paulo")
            .uf("SP")
            .build();
    }
}
