package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Liquida un turno. Toda asignación que quede fuera de {@code closures} se liquida con las horas
 * planificadas.
 */
public record CloseShiftResource(@Valid List<AssignmentClosureResource> closures) {

    /**
     * @param workedHours horas efectivamente trabajadas; se usan las planificadas si se omite
     * @param note        justificación, obligatoria cuando las horas se apartan materialmente del plan
     */
    public record AssignmentClosureResource(
            @NotNull(message = "La asignación es obligatoria")
            Long assignmentId,

            @DecimalMin(value = "0.0", message = "Las horas trabajadas no pueden ser negativas")
            @DecimalMax(value = "24.0", message = "Un turno no puede exceder un día de trabajo")
            BigDecimal workedHours,

            @Size(max = 500)
            String note) {
    }
}
