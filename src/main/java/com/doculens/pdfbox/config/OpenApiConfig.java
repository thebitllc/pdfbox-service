package com.doculens.pdfbox.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pdfBoxOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:8080");
        server.setDescription("Development server");

        Contact contact = new Contact();
        contact.setName("API Support");
        contact.setEmail("support@example.com");

        License license = new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html");

        Info info = new Info()
                .title("PDFBox Service API")
                .version("1.0.0")
                .description("REST API for PDF analysis using Apache PDFBox. " +
                        "This service provides PDF document analysis capabilities including: " +
                        "scanned PDF detection, text extraction with word-level bounding boxes, " +
                        "and metadata extraction. Output format is similar to Google Document AI.")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}
