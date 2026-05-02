package io.github.kbdemiranda.zipcode.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Generic paginated response")
public record PageResponse<T>(
    @Schema(description = "Page content")
    List<T> content,
    @Schema(description = "Current page number (0-based)")
    int page,
    @Schema(description = "Page size")
    int size,
    @Schema(description = "Total number of elements")
    long totalElements,
    @Schema(description = "Total number of pages")
    int totalPages
) {
}
