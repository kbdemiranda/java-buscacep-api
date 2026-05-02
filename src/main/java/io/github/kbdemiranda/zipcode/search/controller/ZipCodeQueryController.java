package io.github.kbdemiranda.zipcode.search.controller;

import io.github.kbdemiranda.zipcode.search.dto.ZipCodeQueryLogResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.ZipCodeQueryLogDetailResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.ErrorResponseDTO;
import io.github.kbdemiranda.zipcode.search.dto.ZipCodeQueryLogFilterDTO;
import io.github.kbdemiranda.zipcode.search.dto.PageResponse;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeProvider;
import io.github.kbdemiranda.zipcode.search.model.ZipCodeQueryStatus;
import io.github.kbdemiranda.zipcode.search.service.ZipCodeService;
import java.time.LocalDateTime;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Zip Code Queries", description = "Endpoints for listing and retrieving zip code queries")
public class ZipCodeQueryController {

    private final ZipCodeService zipCodeService;

    @GetMapping("/zip-code-queries")
    @Operation(
        summary = "List zip code queries with filters",
        description = "Returns paginated zip code queries ordered by request timestamp descending"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logs returned successfully"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid query parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public PageResponse<ZipCodeQueryLogResponseDTO> listQueries(
        @Parameter(description = "Page number (0-based)", example = "0")
        @Min(value = 0, message = "page must be greater than or equal to 0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size", example = "10")
        @Min(value = 1, message = "size must be greater than or equal to 1")
        @RequestParam(defaultValue = "10") int size,
        @Parameter(description = "Zip code filter (with or without mask)", example = "04364030")
        @RequestParam(required = false) String cep,
        @Parameter(description = "Query status filter", example = "SUCCESS")
        @RequestParam(required = false) ZipCodeQueryStatus status,
        @Parameter(description = "Provider filter", example = "WIREMOCK")
        @RequestParam(required = false) ZipCodeProvider provider,
        @Parameter(description = "Filter logs with request timestamp >= dateFrom", example = "2026-04-01T00:00:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @RequestParam(required = false) LocalDateTime dateFrom,
        @Parameter(description = "Filter logs with request timestamp <= dateTo", example = "2026-04-30T23:59:59")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @RequestParam(required = false) LocalDateTime dateTo
    ) {
        String zipCodeInput = cep;
        ZipCodeQueryLogFilterDTO filter = new ZipCodeQueryLogFilterDTO(zipCodeInput, status, provider, dateFrom, dateTo);
        return zipCodeService.listQueries(page, size, filter);
    }

    @GetMapping("/zip-code-queries/{externalId}")
    @Operation(
        summary = "Get zip code query by external id",
        description = "Returns a zip code query by its public external identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Log returned successfully",
            content = @Content(schema = @Schema(implementation = ZipCodeQueryLogDetailResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid UUID",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Log not found",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))
        )
    })
    public ZipCodeQueryLogDetailResponseDTO findByExternalId(
        @Parameter(description = "External public identifier of the zip code query", example = "5dbf0be0-77ff-4c5d-a69f-d8452d58fbd2")
        @PathVariable UUID externalId
    ) {
        return zipCodeService.findByExternalId(externalId);
    }
}
