package com.mineops.mineopsapi.operations.domain.services;

import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.operations.domain.model.commands.AssignOperatorToShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.CancelAssignmentCommand;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentOutcome;

import java.util.Optional;

public interface AssignmentCommandService {

    /**
     * Asigna un operador a una máquina para un turno.
     *
     * @throws com.mineops.mineopsapi.shared.domain.exceptions.BusinessRuleViolationException con todas
     *         las reglas que la asignación incumple, nunca solo la primera
     */
    Optional<AssignmentOutcome> handle(AssignOperatorToShiftCommand command);

    Optional<Shift> handle(CancelAssignmentCommand command);
}
