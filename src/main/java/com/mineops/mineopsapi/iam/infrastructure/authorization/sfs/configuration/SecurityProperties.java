package com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuración de seguridad externalizada, enlazada desde el prefijo {@code mineops.security} y
 * validada al arrancar, para que un despliegue al que le falte un secreto falle de inmediato en
 * lugar de fallar en la primera petición.
 *
 * @param jwt  parámetros de emisión de tokens
 * @param cors orígenes de navegador autorizados a llamar a la API
 */
@Validated
@ConfigurationProperties(prefix = "mineops.security")
public record SecurityProperties(Jwt jwt, Cors cors) {

    /**
     * @param secret          clave HMAC; HS256 exige al menos 32 bytes
     * @param expirationHours vigencia de un token emitido
     * @param issuer          valor que se coloca en el claim {@code iss}
     */
    public record Jwt(@NotBlank String secret, @Min(1) int expirationHours, @NotBlank String issuer) {
    }

    /**
     * @param allowedOrigins orígenes exactos que el navegador puede usar en la petición previa
     */
    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }
}
