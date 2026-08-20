package com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.pipeline;

import com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.model.UsernamePasswordAuthenticationTokenBuilder;
import com.mineops.mineopsapi.iam.infrastructure.tokens.jwt.BearerTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee el token bearer de la petición y, cuando es válido, carga al usuario correspondiente en el
 * contexto de seguridad.
 * <p>
 * Un token ausente o inválido no es un error aquí: la petición simplemente continúa sin autenticar y
 * es la cadena de filtros la que decide si el endpoint destino lo tolera.
 * </p>
 */
public class BearerAuthorizationRequestFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BearerAuthorizationRequestFilter.class);

    private final BearerTokenService tokenService;
    private final UserDetailsService userDetailsService;

    public BearerAuthorizationRequestFilter(BearerTokenService tokenService, UserDetailsService userDetailsService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            var token = tokenService.getBearerTokenFrom(request);
            if (token != null && tokenService.validateToken(token)) {
                var email = tokenService.getEmailFromToken(token);
                var userDetails = userDetailsService.loadUserByUsername(email);
                SecurityContextHolder.getContext()
                        .setAuthentication(UsernamePasswordAuthenticationTokenBuilder.build(userDetails, request));
            }
        } catch (Exception exception) {
            LOGGER.warn("No se pudo establecer el contexto de seguridad: {}", exception.getMessage());
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
