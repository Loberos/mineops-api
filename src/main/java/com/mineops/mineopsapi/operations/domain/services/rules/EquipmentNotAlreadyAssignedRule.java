package com.mineops.mineopsapi.operations.domain.services.rules;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentRuleCode;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Regla de negocio 7: una máquina la conduce como máximo un operador por turno.
 * <p>
 * No es autorizable, por la misma razón que la regla del operador: dos personas no pueden conducir el
 * mismo camión al mismo tiempo.
 * </p>
 */
@Component
@Order(40)
public class EquipmentNotAlreadyAssignedRule implements AssignmentRule {

    @Override
    public Optional<BusinessRuleViolation> evaluate(AssignmentContext context) {
        var equipment = context.equipment();
        if (!context.shift().hasEquipmentAssigned(equipment.id())) {
            return Optional.empty();
        }
        return Optional.of(BusinessRuleViolation.blocking(
                AssignmentRuleCode.EQUIPMENT_ALREADY_ASSIGNED.name(),
                "El equipo %s ya está asignado en este turno".formatted(equipment.code())));
    }
}
