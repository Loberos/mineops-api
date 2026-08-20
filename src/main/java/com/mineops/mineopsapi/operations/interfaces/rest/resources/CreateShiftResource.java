package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.Journey;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateShiftResource(
        @NotNull(message = "La fecha es obligatoria")
        LocalDate date,

        @NotNull(message = "La jornada es obligatoria")
        Journey journey,

        @NotNull(message = "Las horas planificadas son obligatorias")
        @DecimalMin(value = "0.5", message = "Un turno debe durar al menos media hora")
        @DecimalMax(value = "24.0", message = "Un turno no puede durar más de un día")
        BigDecimal plannedHours,

        @Size(max = 500)
        String notes) {
}
