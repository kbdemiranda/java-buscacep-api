package io.github.kbdemiranda.buscacep.controller;

import io.github.kbdemiranda.buscacep.dto.CepResponseDTO;
import io.github.kbdemiranda.buscacep.service.CepService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ceps")
@RequiredArgsConstructor
public class CepController {

    private final CepService cepService;

    @GetMapping("/{cep}")
    public CepResponseDTO findByCep(@PathVariable String cep) {
        return cepService.findCep(cep);
    }
}
