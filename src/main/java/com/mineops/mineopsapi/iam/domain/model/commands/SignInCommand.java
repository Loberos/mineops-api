package com.mineops.mineopsapi.iam.domain.model.commands;

/**
 * Autentica a un usuario y emite un token de acceso.
 *
 * @param email    usuario de acceso
 * @param password contraseña en claro a verificar
 */
public record SignInCommand(String email, String password) {
}
