package com.mineops.mineopsapi.operations.domain.services.rules;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentRuleCode;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Un turno liquidado es historia. Nadie puede agregarle nada, porque sus horas ya se contaron contra
 * los horómetros.
 */
@Component
@Order(10)
public class ShiftMustBeOpenRule implements AssignmentRule {

    @Override
    public Optional<BusinessRuleViolation> evaluate(AssignmentContext context) {
        var shift = context.shift();
        if (shift.isOpen()) {
            return Optional.empty();
        }
        return Optional.of(BusinessRuleViolation.blocking(
                AssignmentRuleCode.SHIFT_NOT_OPEN.name(),
                "El turno del %s (%s) está %s y ya no acepta asignaciones"
                        .formatted(shift.getDate(), shift.getJourney(), shift.getStatus())));
    }
}
