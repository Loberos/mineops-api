package com.mineops.mineopsapi.operations.domain.services;

import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.operations.domain.model.commands.CancelShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.CloseShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.CreateShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.UpdateShiftPlanCommand;

import java.util.Optional;

public interface ShiftCommandService {

    Optional<Shift> handle(CreateShiftCommand command);

    Optional<Shift> handle(UpdateShiftPlanCommand command);

    Optional<Shift> handle(CancelShiftCommand command);

    /**
     * Liquida el turno y empuja las horas trabajadas sobre cada máquina involucrada, que es lo que
     * puede bloquear a alguna de ellas.
     */
    Optional<Shift> handle(CloseShiftCommand command);
}
