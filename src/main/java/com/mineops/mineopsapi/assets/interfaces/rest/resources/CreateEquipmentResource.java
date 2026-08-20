package com.mineops.mineopsapi.assets.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateEquipmentResource(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 40)
        String code,

        @NotNull(message = "El tipo de equipo es obligatorio")
        Long equipmentTypeId,

        @DecimalMin(value = "0.00", message = "El horómetro no puede ser negativo")
        BigDecimal initialHourMeter) {
}
