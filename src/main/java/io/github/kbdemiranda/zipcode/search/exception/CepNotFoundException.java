package io.github.kbdemiranda.zipcode.search.exception;

public class CepNotFoundException extends RuntimeException {

    public CepNotFoundException(String cep) {
        super("CEP não encontrado");
    }
}
