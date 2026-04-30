package io.github.kbdemiranda.buscacep.exception;

import io.github.kbdemiranda.buscacep.model.CepProvider;

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
