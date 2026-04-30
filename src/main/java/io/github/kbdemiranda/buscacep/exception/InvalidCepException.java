package io.github.kbdemiranda.buscacep.exception;

public class InvalidCepException extends RuntimeException {

    public InvalidCepException(String cep) {
        super("Invalid CEP: " + cep + ". CEP must contain 8 digits.");
    }
}
