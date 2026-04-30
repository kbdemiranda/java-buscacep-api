package io.github.kbdemiranda.buscacep.client;

import io.github.kbdemiranda.buscacep.dto.CepResponseDTO;
import java.util.Optional;

public interface CepClient {

    Optional<CepResponseDTO> findByCep(String cep);
}
