package com.mineops.mineopsapi.operations.domain.services.rules;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentRuleCode;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import com.mineops.mineopsapi.workforce.interfaces.acl.CertificationSnapshot;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Regla de negocio 9: un operador solo puede conducir una familia de máquinas para la que esté
 * certificado, y la certificación tiene que estar vigente en la fecha del turno.
 * <p>
 * La regla lee el turno completo, no solo el día en que empieza. Una certificación que vence a
 * medianoche cubre las primeras cinco horas de un turno de noche y ninguna de las demás, y medio
 * turno certificado no es un turno certificado; por eso una certificación que caduca a mitad de
 * camino se informa como su propio caso en vez de aceptarse en silencio.
 * </p>
 */
@Component
@Order(60)
public class OperatorMustBeCertifiedRule implements AssignmentRule {

    @Override
    public Optional<BusinessRuleViolation> evaluate(AssignmentContext context) {
        var operator = context.operator();
        var equipment = context.equipment();
        var shift = context.shift();

        var certification = operator.certificationFor(equipment.equipmentTypeId());
        if (certification.isEmpty()) {
            return Optional.of(BusinessRuleViolation.overridable(
                    AssignmentRuleCode.OPERATOR_NOT_CERTIFIED.name(),
                    "El operador %s no tiene certificación para %s"
                            .formatted(operator.fullName(), equipment.equipmentTypeName())));
        }

        var validity = certification.get();
        var startDate = shift.getDate();
        var endDate = shift.endDate();

        if (!validity.isValidOn(startDate)) {
            return Optional.of(BusinessRuleViolation.overridable(
                    AssignmentRuleCode.OPERATOR_CERTIFICATION_EXPIRED.name(),
                    describeExpired(operator.fullName(), equipment.equipmentTypeName(), validity, startDate)));
        }

        if (!validity.isValidOn(endDate)) {
            return Optional.of(BusinessRuleViolation.overridable(
                    AssignmentRuleCode.CERTIFICATION_EXPIRES_DURING_SHIFT.name(),
                    "La certificación de %s para %s vence el %s, en pleno turno, que termina el %s"
                            .formatted(
                                    operator.fullName(),
                                    equipment.equipmentTypeName(),
                                    validity.expiresOn(),
                                    endDate)));
        }

        return Optional.empty();
    }

    private String describeExpired(
            String operatorName,
            String equipmentTypeName,
            CertificationSnapshot certification,
            LocalDate shiftDate) {
        if (shiftDate.isBefore(certification.issuedOn())) {
            return "La certificación de %s para %s recién entra en vigencia el %s, después de este turno del %s"
                    .formatted(operatorName, equipmentTypeName, certification.issuedOn(), shiftDate);
        }
        return "La certificación de %s para %s venció el %s, antes de este turno del %s"
                .formatted(operatorName, equipmentTypeName, certification.expiresOn(), shiftDate);
    }
}
