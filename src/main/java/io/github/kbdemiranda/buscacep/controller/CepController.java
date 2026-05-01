package io.github.kbdemiranda.buscacep.controller;

import io.github.kbdemiranda.buscacep.dto.CepResponseDTO;
import io.github.kbdemiranda.buscacep.service.CepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ceps")
@RequiredArgsConstructor
@Validated
@Tag(name = "CEPs", description = "Endpoints for CEP lookup")
public class CepController {

    private final CepService cepService;

    @GetMapping("/{cep}")
    @Operation(
        summary = "Search CEP",
        description = "Searches a CEP using WireMock first and ViaCEP as fallback. Each search is logged."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "CEP found"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid CEP",
            content = @Content(schema = @Schema(implementation = io.github.kbdemiranda.buscacep.dto.ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "CEP not found",
            content = @Content(schema = @Schema(implementation = io.github.kbdemiranda.buscacep.dto.ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "502",
            description = "External provider failure",
            content = @Content(schema = @Schema(implementation = io.github.kbdemiranda.buscacep.dto.ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = io.github.kbdemiranda.buscacep.dto.ErrorResponseDTO.class))
        )
    })
    public CepResponseDTO findByCep(
        @Parameter(
            description = "CEP value (8 digits or formatted as 99999-999)",
            example = "04364-030"
        )
        @Pattern(regexp = "^\\d{8}$|^\\d{5}-\\d{3}$", message = "CEP must have 8 digits or format 99999-999")
        @PathVariable String cep
    ) {
        return cepService.findCep(cep);
    }
}
