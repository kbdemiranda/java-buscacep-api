package io.github.kbdemiranda.zipcode.search.exception;

import java.util.UUID;

public class ZipCodeQueryLogNotFoundException extends RuntimeException {

    public ZipCodeQueryLogNotFoundException(UUID externalId) {
        super("Consulta de CEP não encontrada para externalId: " + externalId);
    }
}
