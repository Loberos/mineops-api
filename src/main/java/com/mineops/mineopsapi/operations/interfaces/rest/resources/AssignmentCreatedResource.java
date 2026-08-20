package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import com.mineops.mineopsapi.shared.interfaces.rest.resources.RuleViolationResource;

import java.util.List;

/**
 * La asignación que se creó, más lo que el planificador debería saber sobre ella; lo más frecuente
 * es que este sea el turno que lleva a la máquina más allá de su umbral de mantenimiento.
 */
public record AssignmentCreatedResource(
        AssignmentResource assignment, List<RuleViolationResource> warnings) {
}
