package io.github.kbdemiranda.zipcode.search.client;

import io.github.kbdemiranda.zipcode.search.dto.ZipCodeResponseDTO;
import java.util.Optional;

public interface ZipCodeClient {

    Optional<ZipCodeResponseDTO> searchZipCode(String zipCode);
}
