package io.github.kbdemiranda.zipcode.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Generic paginated response")
public record PageResponse<T>(
    @Schema(description = "Page content")
    List<T> content,
    @Schema(description = "Current page number (0-based)", example = "0")
    int page,
    @Schema(description = "Page size", example = "10")
    int size,
    @Schema(description = "Total number of elements", example = "42")
    long totalElements,
    @Schema(description = "Total number of pages", example = "5")
    int totalPages
) {
}
