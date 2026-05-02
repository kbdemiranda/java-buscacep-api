package io.github.kbdemiranda.zipcode.search.exception;

public class InvalidCepException extends RuntimeException {

    public InvalidCepException(String cep) {
        super("Invalid CEP: " + cep + ". CEP must contain 8 digits.");
    }
}
