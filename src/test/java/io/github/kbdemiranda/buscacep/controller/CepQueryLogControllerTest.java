package io.github.kbdemiranda.buscacep.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.kbdemiranda.buscacep.dto.CepQueryLogResponseDTO;
import io.github.kbdemiranda.buscacep.dto.PageResponse;
import io.github.kbdemiranda.buscacep.service.CepService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CepQueryLogController.class)
@Import(RestExceptionHandler.class)
class CepQueryLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CepService cepService;

    @Test
    void shouldReturnPageMetadataForCustomPagination() throws Exception {
        when(cepService.findAll(0, 10)).thenReturn(buildPageResponse(0, 10, 13, 2));

        mockMvc.perform(get("/api/v1/cep-consultas?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(13))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldReturnRequestedSizeWhenSizeIsFive() throws Exception {
        when(cepService.findAll(0, 5)).thenReturn(buildPageResponse(0, 5, 13, 3));

        mockMvc.perform(get("/api/v1/cep-consultas?page=0&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void shouldReturnRequestedPageWhenPageIsOne() throws Exception {
        when(cepService.findAll(1, 5)).thenReturn(buildPageResponse(1, 5, 13, 3));

        mockMvc.perform(get("/api/v1/cep-consultas?page=1&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void shouldUseDefaultPaginationWhenParametersAreNotProvided() throws Exception {
        when(cepService.findAll(0, 10)).thenReturn(buildPageResponse(0, 10, 13, 2));

        mockMvc.perform(get("/api/v1/cep-consultas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10));

        verify(cepService).findAll(eq(0), eq(10));
    }

    private static PageResponse<CepQueryLogResponseDTO> buildPageResponse(int page, int size, long totalElements, int totalPages) {
        CepQueryLogResponseDTO log = CepQueryLogResponseDTO.builder()
            .externalId(UUID.randomUUID())
            .cep("04364030")
            .provider("WIREMOCK")
            .status("SUCCESS")
            .requestTimestamp(LocalDateTime.now())
            .build();
        return new PageResponse<>(List.of(log), page, size, totalElements, totalPages);
    }
}
