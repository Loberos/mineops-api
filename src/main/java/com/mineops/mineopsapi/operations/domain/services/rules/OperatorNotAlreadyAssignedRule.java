package com.mineops.mineopsapi.operations.domain.services.rules;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentRuleCode;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Regla de negocio 6: un operador, una máquina, un turno.
 * <p>
 * No es autorizable. Una persona no puede estar en dos cabinas a la vez, así que ninguna
 * autorización volvería verdadera la dotación resultante.
 * </p>
 */
@Component
@Order(30)
public class OperatorNotAlreadyAssignedRule implements AssignmentRule {

    @Override
    public Optional<BusinessRuleViolation> evaluate(AssignmentContext context) {
        var operator = context.operator();
        if (!context.shift().hasOperatorAssigned(operator.id())) {
            return Optional.empty();
        }
        return Optional.of(BusinessRuleViolation.blocking(
                AssignmentRuleCode.OPERATOR_ALREADY_ASSIGNED.name(),
                "El operador %s ya tiene una asignación en este turno".formatted(operator.fullName())));
    }
}
