package com.mineops.mineopsapi.operations.domain.model.queries;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.ShiftStatus;

import java.time.LocalDate;

/**
 * Lista turnos dentro de una ventana.
 *
 * @param from   primer día a incluir; null significa sin límite inferior
 * @param to     último día a incluir; null significa sin límite superior
 * @param status deja solo los turnos en este estado; null deja todos los estados
 */
public record GetAllShiftsQuery(LocalDate from, LocalDate to, ShiftStatus status) {

    public static GetAllShiftsQuery unfiltered() {
        return new GetAllShiftsQuery(null, null, null);
    }
}
