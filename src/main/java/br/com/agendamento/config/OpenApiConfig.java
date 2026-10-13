package br.com.agendamento.config;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.*;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info=@Info(title="Agendamento API", version="1.0.0",
    description="Agenda por empresa, com servicos e profissionais"), security=@SecurityRequirement(name="bearerAuth"))
@SecurityScheme(name="bearerAuth", type=SecuritySchemeType.HTTP, scheme="bearer", bearerFormat="JWT")
public class OpenApiConfig {}
