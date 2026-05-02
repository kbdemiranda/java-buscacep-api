package io.github.kbdemiranda.zipcode.search.exception;

public class InvalidZipCodeException extends RuntimeException {

    public InvalidZipCodeException(String cep) {
        super("Invalid CEP: " + cep + ". CEP must contain 8 digits.");
    }
}
