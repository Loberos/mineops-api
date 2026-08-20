package com.mineops.mineopsapi.assets.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param performedOn  fecha en que se ejecutó el trabajo; hoy si se omite
 * @param hourMeter    lectura tomada por el taller; se usa la lectura actual si se omite
 * @param responsible  quién ejecutó o dio conformidad al trabajo
 * @param observations notas libres
 */
public record RegisterMaintenanceResource(
        LocalDate performedOn,

        @DecimalMin(value = "0.00", message = "El horómetro no puede ser negativo")
        BigDecimal hourMeter,

        @NotBlank(message = "El responsable es obligatorio")
        @Size(max = 120)
        String responsible,

        @Size(max = 1000)
        String observations) {
}
