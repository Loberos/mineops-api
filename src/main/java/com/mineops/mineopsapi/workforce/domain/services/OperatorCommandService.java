package com.mineops.mineopsapi.workforce.domain.services;

import com.mineops.mineopsapi.workforce.domain.model.aggregates.Operator;
import com.mineops.mineopsapi.workforce.domain.model.commands.ChangeOperatorStatusCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.CreateOperatorCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.GrantCertificationCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.RevokeCertificationCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.UpdateOperatorCommand;

import java.util.Optional;

/**
 * Lado de escritura del agregado de operador.
 * <p>
 * Las certificaciones se otorgan y revocan desde aquí y no desde un servicio propio, porque
 * pertenecen al agregado de operador y sus invariantes solo pueden verificarse desde él.
 * </p>
 */
public interface OperatorCommandService {

    Optional<Operator> handle(CreateOperatorCommand command);

    Optional<Operator> handle(UpdateOperatorCommand command);

    Optional<Operator> handle(ChangeOperatorStatusCommand command);

    Optional<Operator> handle(GrantCertificationCommand command);

    Optional<Operator> handle(RevokeCertificationCommand command);
}
