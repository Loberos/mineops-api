package com.mineops.mineopsapi.operations.domain.model.valueobjects;

import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;

import java.util.List;

/**
 * Veredicto sobre una asignación propuesta: todas las reglas que incumple, no solo la primera.
 *
 * @param violations todas las violaciones detectadas, en el orden en que se declararon las reglas
 */
public record AssignmentEvaluation(List<BusinessRuleViolation> violations) {

    public AssignmentEvaluation {
        violations = List.copyOf(violations);
    }

    public static AssignmentEvaluation accepted() {
        return new AssignmentEvaluation(List.of());
    }

    /**
     * Indica si la asignación puede proceder sin que nadie tenga que autorizarla.
     */
    public boolean isAccepted() {
        return blockingViolations().isEmpty();
    }

    public List<BusinessRuleViolation> blockingViolations() {
        return violations.stream().filter(BusinessRuleViolation::isBlocking).toList();
    }

    /**
     * Violaciones que ninguna autoridad puede levantar, porque la dotación resultante sería imposible
     * y no meramente riesgosa: la misma máquina conducida por dos personas a la vez, por ejemplo.
     */
    public List<BusinessRuleViolation> nonOverridableViolations() {
        return blockingViolations().stream()
                .filter(violation -> !violation.overridable())
                .toList();
    }

    /**
     * Cosas que el planificador debería saber pero que no impiden la asignación.
     */
    public List<BusinessRuleViolation> warnings() {
        return violations.stream().filter(violation -> !violation.isBlocking()).toList();
    }

    /**
     * Indica si un supervisor puede autorizar esta asignación tal como está.
     */
    public boolean canBeOverridden() {
        return !isAccepted() && nonOverridableViolations().isEmpty();
    }
}
