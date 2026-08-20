package com.mineops.mineopsapi.workforce.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOperatorResource(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80)
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 80)
        String lastName) {
}
