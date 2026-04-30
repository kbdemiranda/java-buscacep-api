package io.github.kbdemiranda.buscacep.controller;

import io.github.kbdemiranda.buscacep.dto.CepQueryLogResponseDTO;
import io.github.kbdemiranda.buscacep.dto.ErrorResponseDTO;
import io.github.kbdemiranda.buscacep.dto.PageResponse;
import io.github.kbdemiranda.buscacep.service.CepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "CEP Query Logs", description = "Endpoints for listing CEP query logs")
public class CepQueryLogController {

    private final CepService cepService;

    @GetMapping("/cep-consultas")
    @Operation(
        summary = "List CEP query logs",
        description = "Returns paginated CEP query logs ordered by request timestamp descending"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logs returned successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid pagination parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public PageResponse<CepQueryLogResponseDTO> findAll(
        @Parameter(description = "Page number (0-based)", example = "0")
        @Min(value = 0, message = "page must be greater than or equal to 0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size", example = "10")
        @Min(value = 1, message = "size must be greater than or equal to 1")
        @RequestParam(defaultValue = "10") int size
    ) {
        return cepService.findAll(page, size);
    }
}
