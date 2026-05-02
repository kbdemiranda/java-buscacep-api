package io.github.kbdemiranda.zipcode.search.exception;

import java.util.UUID;

public class CepQueryLogNotFoundException extends RuntimeException {

    public CepQueryLogNotFoundException(UUID externalId) {
        super("Consulta de CEP não encontrada para externalId: " + externalId);
    }
}
