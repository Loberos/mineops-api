package com.mineops.mineopsapi.shared.domain.model.valueobjects;

/**
 * Una regla de negocio que un comando no logró satisfacer.
 * <p>
 * Las violaciones son valores, no excepciones: los evaluadores las devuelven para que un comando
 * pueda contrastarse contra todas las reglas e informar de una sola vez todas las razones por las
 * que se rechaza, en lugar de abortar en el primer incumplimiento.
 * </p>
 *
 * @param code        identificador estable y legible por máquina, seguro para ramificar desde el cliente
 * @param message     explicación legible por una persona, lista para mostrarse
 * @param severity    si la violación impide el comando o solo advierte sobre él
 * @param overridable si un supervisor puede autorizar el comando pese a esta violación
 */
public record BusinessRuleViolation(String code, String message, RuleSeverity severity, boolean overridable) {

    public BusinessRuleViolation {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Una violación de regla de negocio requiere un código");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Una violación de regla de negocio requiere un mensaje");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Una violación de regla de negocio requiere una severidad");
        }
    }

    /**
     * Crea una violación que impide el comando y que nadie puede levantar, porque el estado
     * resultante sería físicamente imposible y no meramente riesgoso.
     */
    public static BusinessRuleViolation blocking(String code, String message) {
        return new BusinessRuleViolation(code, message, RuleSeverity.BLOCKING, false);
    }

    /**
     * Crea una violación que impide el comando pero que un supervisor puede autorizar de forma
     * explícita, dejando esa autorización en la traza de auditoría.
     */
    public static BusinessRuleViolation overridable(String code, String message) {
        return new BusinessRuleViolation(code, message, RuleSeverity.BLOCKING, true);
    }

    /**
     * Crea una violación que no impide el comando pero que debe mostrarse a quien lo ejecuta.
     */
    public static BusinessRuleViolation warning(String code, String message) {
        return new BusinessRuleViolation(code, message, RuleSeverity.WARNING, true);
    }

    public boolean isBlocking() {
        return severity == RuleSeverity.BLOCKING;
    }
}
