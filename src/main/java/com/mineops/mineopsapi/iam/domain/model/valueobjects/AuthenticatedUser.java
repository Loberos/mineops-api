package com.mineops.mineopsapi.iam.domain.model.valueobjects;

import com.mineops.mineopsapi.iam.domain.model.aggregates.User;

/**
 * Resultado de un inicio de sesión exitoso: el usuario autenticado junto con el token de acceso
 * emitido para la sesión.
 *
 * @param user  el usuario autenticado
 * @param token el token bearer firmado
 */
public record AuthenticatedUser(User user, String token) {
}
