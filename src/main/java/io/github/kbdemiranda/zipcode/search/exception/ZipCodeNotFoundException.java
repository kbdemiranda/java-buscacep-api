package io.github.kbdemiranda.zipcode.search.exception;

public class ZipCodeNotFoundException extends RuntimeException {

    public ZipCodeNotFoundException(String cep) {
        super("CEP não encontrado");
    }
}
