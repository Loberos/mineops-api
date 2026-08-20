package com.mineops.mineopsapi.operations.interfaces.rest.resources;

import com.mineops.mineopsapi.shared.interfaces.rest.resources.RuleViolationResource;

import java.util.List;

/**
 * Qué pasaría si se enviara una asignación propuesta.
 *
 * @param accepted        si pasaría sin necesitar autorización alguna
 * @param canBeOverridden si un supervisor puede autorizarla tal como está
 * @param violations      todas las reglas que incumple, no solo la primera
 * @param warnings        cosas que conviene saber pero que no la impiden
 */
public record AssignmentEvaluationResource(
        boolean accepted,
        boolean canBeOverridden,
        List<RuleViolationResource> violations,
        List<RuleViolationResource> warnings) {
}
