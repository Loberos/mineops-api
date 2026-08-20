package com.mineops.mineopsapi.assets.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateEquipmentTypeResource(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        String name,

        @NotNull(message = "El intervalo de mantenimiento es obligatorio")
        @DecimalMin(value = "0.01", message = "El intervalo de mantenimiento debe ser mayor que cero")
        BigDecimal maintenanceIntervalHours,

        @Size(max = 400)
        String description) {
}
