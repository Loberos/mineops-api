package com.mineops.mineopsapi.iam.application.internal.outboundservices.hashing;

/**
 * Puerto de salida para el hasheo de contraseñas. La capa de aplicación depende de esta abstracción
 * para que el algoritmo pueda reemplazarse sin tocar ningún caso de uso.
 */
public interface HashingService {

    String encode(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String encodedPassword);
}
