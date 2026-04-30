package io.github.kbdemiranda.buscacep.controller;

import io.github.kbdemiranda.buscacep.dto.CepQueryLogResponseDTO;
import io.github.kbdemiranda.buscacep.dto.PageResponse;
import io.github.kbdemiranda.buscacep.service.CepService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CepQueryLogController {

    private final CepService cepService;

    @GetMapping("/cep-consultas")
    public PageResponse<CepQueryLogResponseDTO> findAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return cepService.findAll(page, size);
    }
}
