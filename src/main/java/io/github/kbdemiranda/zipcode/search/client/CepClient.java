package io.github.kbdemiranda.zipcode.search.client;

import io.github.kbdemiranda.zipcode.search.dto.CepResponseDTO;
import java.util.Optional;

public interface CepClient {

    Optional<CepResponseDTO> findByCep(String cep);
}
