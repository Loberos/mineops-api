package com.mineops.mineopsapi.workforce.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Certifica a un operador para una familia de máquinas. Enviarlo dos veces para la misma familia
 * renueva la certificación existente en lugar de crear una segunda.
 */
public record GrantCertificationResource(
        @NotNull(message = "El tipo de equipo es obligatorio")
        Long equipmentTypeId,

        @NotNull(message = "La fecha de emisión es obligatoria")
        LocalDate issuedOn,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        LocalDate expiresOn) {
}
