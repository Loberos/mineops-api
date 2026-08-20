package com.mineops.mineopsapi.shared.interfaces.rest.resources;

import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;

/**
 * Representación de transporte de una única regla de negocio incumplida.
 *
 * @param code        identificador estable sobre el que el cliente puede ramificar
 * @param message     explicación lista para mostrarse
 * @param severity    {@code BLOCKING} o {@code WARNING}
 * @param overridable si un supervisor puede autorizar el comando pese a esta violación
 */
public record RuleViolationResource(String code, String message, String severity, boolean overridable) {

    public static RuleViolationResource fromViolation(BusinessRuleViolation violation) {
        return new RuleViolationResource(
                violation.code(),
                violation.message(),
                violation.severity().name(),
                violation.overridable());
    }
}
