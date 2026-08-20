package com.mineops.mineopsapi.operations.domain.model.valueobjects;

/**
 * Identificadores estables de las reglas contra las que se contrasta una asignación.
 * <p>
 * Forman parte del contrato con el cliente: el frontend agrupa y colorea los rechazos por estos
 * códigos, así que pueden sumarse nuevos pero no deben renombrarse.
 * </p>
 */
public enum AssignmentRuleCode {

    /** El turno ya fue cerrado o suspendido. */
    SHIFT_NOT_OPEN,

    /** El operador ya no forma parte de la plantilla. */
    OPERATOR_INACTIVE,

    /** El operador ya conduce otra máquina en este turno. */
    OPERATOR_ALREADY_ASSIGNED,

    /** La máquina ya la conduce otra persona en este turno. */
    EQUIPMENT_ALREADY_ASSIGNED,

    /** La máquina está bloqueada, en el taller o retirada de servicio. */
    EQUIPMENT_NOT_AVAILABLE,

    /** El operador no tiene ninguna certificación para esta familia de máquinas. */
    OPERATOR_NOT_CERTIFIED,

    /** El operador tiene certificación para esta familia, pero no está vigente en la fecha del turno. */
    OPERATOR_CERTIFICATION_EXPIRED,

    /** La certificación está vigente al comenzar el turno pero vence antes de que termine. */
    CERTIFICATION_EXPIRES_DURING_SHIFT,

    /** Trabajar este turno llevaría a la máquina más allá de su umbral de mantenimiento. */
    EQUIPMENT_WILL_REACH_THRESHOLD
}
