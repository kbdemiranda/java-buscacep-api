package io.github.kbdemiranda.zipcode.search.exception;

import io.github.kbdemiranda.zipcode.search.model.CepProvider;

public class ExternalCepClientException extends RuntimeException {

    private final CepProvider provider;

    public ExternalCepClientException(CepProvider provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public CepProvider getProvider() {
        return provider;
    }
}
