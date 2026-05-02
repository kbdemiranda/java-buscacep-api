package io.github.kbdemiranda.zipcode.search.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kbdemiranda.zipcode.search.dto.ZipCodeQueryLogResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.ZipCodeQueryLogDetailResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.ZipCodeQueryLogFilterDTO;
import io.github.kbdemiranda.zipcode.search.dto.PageResponse;
import io.github.kbdemiranda.zipcode.search.exception.ZipCodeQueryLogNotFoundException;
import io.github.kbdemiranda.zipcode.search.service.ZipCodeService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ZipCodeQueryController.class)
@Import(GlobalExceptionHandler.class)
class ZipCodeQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZipCodeService zipCodeService;

    @Test
    void shouldReturnPageMetadataForCustomPagination() throws Exception {
        when(zipCodeService.listQueries(eq(0), eq(10), any(ZipCodeQueryLogFilterDTO.class))).thenReturn(buildPageResponse(0, 10, 13, 2));

        mockMvc.perform(get("/api/v1/zip-code-queries?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(13))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldReturnRequestedSizeWhenSizeIsFive() throws Exception {
        when(zipCodeService.listQueries(eq(0), eq(5), any(ZipCodeQueryLogFilterDTO.class))).thenReturn(buildPageResponse(0, 5, 13, 3));

        mockMvc.perform(get("/api/v1/zip-code-queries?page=0&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void shouldReturnRequestedPageWhenPageIsOne() throws Exception {
        when(zipCodeService.listQueries(eq(1), eq(5), any(ZipCodeQueryLogFilterDTO.class))).thenReturn(buildPageResponse(1, 5, 13, 3));

        mockMvc.perform(get("/api/v1/zip-code-queries?page=1&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void shouldUseDefaultPaginationWhenParametersAreNotProvided() throws Exception {
        when(zipCodeService.listQueries(eq(0), eq(10), any(ZipCodeQueryLogFilterDTO.class))).thenReturn(buildPageResponse(0, 10, 13, 2));

        mockMvc.perform(get("/api/v1/zip-code-queries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10));

        verify(zipCodeService).listQueries(eq(0), eq(10), any(ZipCodeQueryLogFilterDTO.class));
    }

    @Test
    void shouldReturn400WhenStatusParamIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/zip-code-queries?status=DONE"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Parâmetro inválido"))
            .andExpect(jsonPath("$.path").value("/api/v1/zip-code-queries"));
    }

    @Test
    void shouldFindZipCodeQueryByExternalId() throws Exception {
        UUID externalId = UUID.randomUUID();
        when(zipCodeService.findByExternalId(externalId)).thenReturn(
            ZipCodeQueryLogDetailResponseDTO.builder()
                .externalId(externalId)
                .zipCode("04364030")
                .provider("WIREMOCK")
                .status("SUCCESS")
                .requestTimestamp(LocalDateTime.of(2026, 4, 30, 15, 22, 0))
                .errorMessage(null)
                .createdAt(LocalDateTime.of(2026, 4, 30, 15, 22, 1))
                .updatedAt(LocalDateTime.of(2026, 4, 30, 15, 22, 2))
                .build()
        );

        mockMvc.perform(get("/api/v1/zip-code-queries/{externalId}", externalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.externalId").value(externalId.toString()))
            .andExpect(jsonPath("$.cep").value("04364030"))
            .andExpect(jsonPath("$.provider").value("WIREMOCK"))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void shouldReturn404WhenExternalIdDoesNotExist() throws Exception {
        UUID externalId = UUID.randomUUID();
        when(zipCodeService.findByExternalId(externalId)).thenThrow(new ZipCodeQueryLogNotFoundException(externalId));

        mockMvc.perform(get("/api/v1/zip-code-queries/{externalId}", externalId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Consulta de CEP não encontrada"))
            .andExpect(jsonPath("$.path").value("/api/v1/zip-code-queries/" + externalId));
    }

    @Test
    void shouldReturn400WhenExternalIdIsInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/zip-code-queries/{externalId}", "not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Parâmetro inválido"))
            .andExpect(jsonPath("$.path").value("/api/v1/zip-code-queries/not-a-uuid"));
    }

    private static PageResponse<ZipCodeQueryLogResponseDTO> buildPageResponse(int page, int size, long totalElements, int totalPages) {
        ZipCodeQueryLogResponseDTO log = ZipCodeQueryLogResponseDTO.builder()
            .externalId(UUID.randomUUID())
            .zipCode("04364030")
            .provider("WIREMOCK")
            .status("SUCCESS")
            .requestTimestamp(LocalDateTime.now())
            .build();
        return new PageResponse<>(List.of(log), page, size, totalElements, totalPages);
    }
}
