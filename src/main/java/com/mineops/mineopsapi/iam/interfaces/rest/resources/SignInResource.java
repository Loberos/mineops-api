package com.mineops.mineopsapi.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignInResource(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato del correo no es válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password) {
}
