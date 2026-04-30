package io.github.kbdemiranda.buscacep.exception;

public class CepNotFoundException extends RuntimeException {

    public CepNotFoundException(String cep) {
        super("CEP not found: " + cep);
    }
}
