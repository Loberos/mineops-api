package com.mineops.mineopsapi.iam.infrastructure.tokens.jwt;

import com.mineops.mineopsapi.iam.application.internal.outboundservices.tokens.TokenService;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Puerto de tokens especializado para el esquema bearer de HTTP.
 */
public interface BearerTokenService extends TokenService {

    /**
     * Extrae el token de la cabecera {@code Authorization}.
     *
     * @param request la petición entrante
     * @return el token en crudo, o {@code null} cuando la cabecera está ausente o mal formada
     */
    String getBearerTokenFrom(HttpServletRequest request);
}
