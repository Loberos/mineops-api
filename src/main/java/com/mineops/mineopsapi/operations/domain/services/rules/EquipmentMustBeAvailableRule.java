package com.mineops.mineopsapi.operations.domain.services.rules;

import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentRuleCode;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Regla de negocio 8: una máquina bloqueada, en el taller o retirada no puede programarse.
 * <p>
 * Esta sí es autorizable. Una máquina treinta horas pasada de su umbral un viernes por la noche, con
 * el taller abriendo el lunes, es una decisión real que un supervisor tiene que poder tomar; quitarle
 * esa decisión solo consigue que se tome en una hoja de cálculo. Lo que el sistema exige es que la
 * decisión quede firmada.
 * </p>
 */
@Component
@Order(50)
public class EquipmentMustBeAvailableRule implements AssignmentRule {

    @Override
    public Optional<BusinessRuleViolation> evaluate(AssignmentContext context) {
        var equipment = context.equipment();
        if (equipment.isAvailableForAssignment()) {
            return Optional.empty();
        }
        return Optional.of(BusinessRuleViolation.overridable(
                AssignmentRuleCode.EQUIPMENT_NOT_AVAILABLE.name(), describe(equipment.code(), equipment.status())));
    }

    private String describe(String code, EquipmentStatus status) {
        return switch (status) {
            case BLOCKED -> "El equipo %s está bloqueado: alcanzó su umbral de mantenimiento".formatted(code);
            case IN_MAINTENANCE -> "El equipo %s está en el taller".formatted(code);
            case OUT_OF_SERVICE -> "El equipo %s está retirado de servicio".formatted(code);
            case AVAILABLE -> "El equipo %s está disponible".formatted(code);
        };
    }
}
