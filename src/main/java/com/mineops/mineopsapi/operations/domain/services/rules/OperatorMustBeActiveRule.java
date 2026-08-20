package com.mineops.mineopsapi.operations.domain.services.rules;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentRuleCode;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Un operador que dejó la plantilla no puede programarse. No es un riesgo que un supervisor pueda
 * aceptar: la persona sencillamente no está.
 */
@Component
@Order(20)
public class OperatorMustBeActiveRule implements AssignmentRule {

    @Override
    public Optional<BusinessRuleViolation> evaluate(AssignmentContext context) {
        var operator = context.operator();
        if (operator.isAvailableForAssignment()) {
            return Optional.empty();
        }
        return Optional.of(BusinessRuleViolation.blocking(
                AssignmentRuleCode.OPERATOR_INACTIVE.name(),
                "El operador %s está inactivo y no puede programarse".formatted(operator.fullName())));
    }
}
