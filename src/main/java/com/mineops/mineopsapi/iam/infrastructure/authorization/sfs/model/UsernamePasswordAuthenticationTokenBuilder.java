package com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.model;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

/**
 * Construye el objeto de autenticación que se coloca en el contexto de seguridad una vez verificado
 * el token bearer. Las credenciales se dejan nulas a propósito: el token ya probó la identidad.
 */
public final class UsernamePasswordAuthenticationTokenBuilder {

    private UsernamePasswordAuthenticationTokenBuilder() {
    }

    public static UsernamePasswordAuthenticationToken build(UserDetails principal, HttpServletRequest request) {
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }
}
