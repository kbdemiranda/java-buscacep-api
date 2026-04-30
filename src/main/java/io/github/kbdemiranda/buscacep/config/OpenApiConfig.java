package io.github.kbdemiranda.buscacep.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Busca CEP API")
                .description("API for Brazilian CEP lookup using WireMock as primary provider and ViaCEP as fallback")
                .version("v1")
                .contact(new Contact().name("Busca CEP Team")))
            .servers(List.of(new Server().url("http://localhost:8080")));
    }
}
