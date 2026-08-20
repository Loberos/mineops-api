package com.mineops.mineopsapi.operations.domain.model.commands;

import java.math.BigDecimal;
import java.util.List;

/**
 * Liquida un turno: registra lo que cada asignación trabajó realmente y suma esas horas al horómetro
 * de cada máquina involucrada, lo que puede llevar a alguna más allá de su umbral.
 *
 * @param shiftId  el turno que se cierra
 * @param closures las horas trabajadas, una entrada por asignación abierta
 */
public record CloseShiftCommand(Long shiftId, List<AssignmentClosure> closures) {

    /**
     * La liquidación de una asignación.
     *
     * @param assignmentId la asignación
     * @param workedHours  horas efectivamente trabajadas; se usan las planificadas si se omite
     * @param note         justificación, obligatoria cuando las horas se apartan materialmente del plan
     */
    public record AssignmentClosure(Long assignmentId, BigDecimal workedHours, String note) {
    }
}
