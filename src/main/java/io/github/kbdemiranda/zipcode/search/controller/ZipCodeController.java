package io.github.kbdemiranda.zipcode.search.controller;

import io.github.kbdemiranda.zipcode.search.dto.ZipCodeResponseDTO;
import io.github.kbdemiranda.zipcode.search.service.ZipCodeService;
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
@RequestMapping("/api/v1/zip-codes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Zip Codes", description = "Endpoints for zip code lookup")
public class ZipCodeController {

    private final ZipCodeService zipCodeService;

    @GetMapping("/{cep}")
    @Operation(
        summary = "Search zip code",
        description = "Searches a zip code using WireMock first and ViaCEP as fallback. Each search is logged."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Zip code found"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid zip code",
            content = @Content(schema = @Schema(implementation = io.github.kbdemiranda.zipcode.search.dto.ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Zip code not found",
            content = @Content(schema = @Schema(implementation = io.github.kbdemiranda.zipcode.search.dto.ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "502",
            description = "External provider error",
            content = @Content(schema = @Schema(implementation = io.github.kbdemiranda.zipcode.search.dto.ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected internal server error",
            content = @Content(schema = @Schema(implementation = io.github.kbdemiranda.zipcode.search.dto.ErrorResponseDTO.class))
        )
    })
    public ZipCodeResponseDTO searchZipCode(
        @Parameter(
            description = "Zip code value (8 digits or formatted as 99999-999)"
        )
        @Pattern(regexp = "^\\d{8}$|^\\d{5}-\\d{3}$", message = "CEP must have 8 digits or format 99999-999")
        @PathVariable String cep
    ) {
        String zipCodeInput = cep;
        return zipCodeService.searchZipCode(zipCodeInput);
    }
}
