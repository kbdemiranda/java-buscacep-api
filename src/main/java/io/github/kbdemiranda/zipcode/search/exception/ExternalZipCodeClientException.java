package io.github.kbdemiranda.zipcode.search.exception;

import io.github.kbdemiranda.zipcode.search.model.ZipCodeProvider;

public class ExternalZipCodeClientException extends RuntimeException {

    private final ZipCodeProvider provider;

    public ExternalZipCodeClientException(ZipCodeProvider provider, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    public ZipCodeProvider getProvider() {
        return provider;
    }
}
