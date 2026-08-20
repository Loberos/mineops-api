package com.mineops.mineopsapi.operations.domain.model.commands;

/**
 * Asigna un operador a una máquina para un turno.
 *
 * @param shiftId             el turno
 * @param operatorId          el operador
 * @param equipmentId         la máquina
 * @param forced              si quien llama está autorizando la asignación pese a las reglas incumplidas
 * @param authorizationReason por qué se hace la excepción; obligatorio cuando se fuerza
 * @param requestedByEmail    usuario que emite el comando, que se resuelve contra el contexto de
 *                            identidad para decidir si puede autorizar una excepción y para firmarla
 */
public record AssignOperatorToShiftCommand(
        Long shiftId,
        Long operatorId,
        Long equipmentId,
        boolean forced,
        String authorizationReason,
        String requestedByEmail) {

    /**
     * Una asignación normal, que se espera cumpla todas las reglas.
     */
    public static AssignOperatorToShiftCommand plain(
            Long shiftId, Long operatorId, Long equipmentId, String requestedByEmail) {
        return new AssignOperatorToShiftCommand(shiftId, operatorId, equipmentId, false, null, requestedByEmail);
    }
}
