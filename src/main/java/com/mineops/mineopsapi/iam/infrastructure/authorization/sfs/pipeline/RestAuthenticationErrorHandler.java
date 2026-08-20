package com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.pipeline;

import com.mineops.mineopsapi.shared.interfaces.rest.resources.ApiErrorResource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Renderiza los fallos que ocurren dentro de la cadena de filtros de seguridad usando el mismo
 * envoltorio de error que usan los controladores, para que un cliente nunca tenga que interpretar
 * dos formatos de error distintos.
 */
@Component
public class RestAuthenticationErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestAuthenticationErrorHandler.class);

    private final ObjectMapper objectMapper;

    public RestAuthenticationErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Sin credenciales, o con credenciales que no se pudieron verificar.
     */
    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        LOGGER.debug("Petición sin autenticar a {}", request.getRequestURI());
        write(request, response, HttpStatus.UNAUTHORIZED, "Se requiere autenticación para acceder a este recurso");
    }

    /**
     * Credenciales válidas, pero los roles otorgados no alcanzan para este endpoint.
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException {
        LOGGER.info("Acceso denegado a {}", request.getRequestURI());
        write(request, response, HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta operación");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(), ApiErrorResource.of(status, message, request.getRequestURI()));
    }
}
