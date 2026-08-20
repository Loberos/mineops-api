package com.mineops.mineopsapi.shared.domain.model.valueobjects;

/**
 * Peso que tiene una {@link BusinessRuleViolation} al evaluar un comando.
 */
public enum RuleSeverity {

    /** El comando no puede ejecutarse mientras la violación se mantenga. */
    BLOCKING,

    /** El comando se permite, pero hay que advertir de la consecuencia a quien lo ejecuta. */
    WARNING
}
