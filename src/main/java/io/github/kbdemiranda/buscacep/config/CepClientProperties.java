package io.github.kbdemiranda.buscacep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cep.client")
public record CepClientProperties(
    String wiremockUrl,
    String viacepUrl
) {
}
