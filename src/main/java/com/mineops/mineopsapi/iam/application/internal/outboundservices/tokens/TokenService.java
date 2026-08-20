package com.mineops.mineopsapi.iam.application.internal.outboundservices.tokens;

/**
 * Puerto de salida para emitir y validar tokens de acceso.
 */
public interface TokenService {

    String generateToken(String email);

    String getEmailFromToken(String token);

    boolean validateToken(String token);
}
