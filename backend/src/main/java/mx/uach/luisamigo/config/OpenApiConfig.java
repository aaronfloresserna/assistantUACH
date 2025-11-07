package mx.uach.luisamigo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI luisAmigoOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Luis Amigo - API de Asistente Jurídico UACH")
                .description("API REST para el asistente jurídico académico basado en RAG")
                .version("0.1.0")
                .license(new License()
                    .name("Academic Use - CC BY-NC 4.0")
                    .url("https://creativecommons.org/licenses/by-nc/4.0/")));
    }
}
