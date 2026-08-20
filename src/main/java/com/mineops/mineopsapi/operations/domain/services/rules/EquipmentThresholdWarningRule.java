package com.mineops.mineopsapi.operations.domain.services.rules;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentRuleCode;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Advierte cuando el turno que se está planificando es el que llevará a la máquina más allá de su
 * umbral.
 * <p>
 * No impide nada: la máquina está disponible hoy y trabajarla es legítimo. Se informa para que el
 * planificador reserve taller antes de que la máquina se bloquee sola, en vez de enterarse la mañana
 * en que la programación se le cae.
 * </p>
 */
@Component
@Order(70)
public class EquipmentThresholdWarningRule implements AssignmentRule {

    @Override
    public Optional<BusinessRuleViolation> evaluate(AssignmentContext context) {
        var equipment = context.equipment();
        if (!equipment.isAvailableForAssignment()) {
            // Ya se informó como violación bloqueante; decirlo dos veces no agrega nada.
            return Optional.empty();
        }
        var plannedHours = context.shift().getPlannedHours();
        if (!equipment.wouldReachThresholdAfter(plannedHours)) {
            return Optional.empty();
        }
        return Optional.of(BusinessRuleViolation.warning(
                AssignmentRuleCode.EQUIPMENT_WILL_REACH_THRESHOLD.name(),
                "Después de este turno, el equipo %s llega a %s horas y a su umbral de mantenimiento de %s horas. "
                        .formatted(
                                equipment.code(),
                                equipment.hourMeter().add(plannedHours),
                                equipment.maintenanceThresholdHours())
                        + "Programa su mantenimiento."));
    }
}
