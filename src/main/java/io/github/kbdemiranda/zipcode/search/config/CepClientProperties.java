package io.github.kbdemiranda.zipcode.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cep.client")
public record CepClientProperties(
    String wiremockUrl,
    String viacepUrl
) {
}
