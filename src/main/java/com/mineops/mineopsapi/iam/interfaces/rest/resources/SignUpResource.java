package com.mineops.mineopsapi.iam.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SignUpResource(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato del correo no es válido")
        @Size(max = 160)
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 120)
        String fullName,

        /** Nombres de rol como {@code SUPERVISOR}. Se otorga el rol de consulta si se omite. */
        List<String> roles) {
}
