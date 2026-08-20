package com.mineops.mineopsapi.iam.infrastructure.hashing.bcrypt;

import com.mineops.mineopsapi.iam.application.internal.outboundservices.hashing.HashingService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Variante BCrypt del puerto de hasheo. Cumple además el contrato {@link PasswordEncoder} de Spring
 * Security, de modo que una sola implementación sirve tanto a la capa de aplicación como al
 * framework.
 */
public interface BCryptHashingService extends HashingService, PasswordEncoder {
}
