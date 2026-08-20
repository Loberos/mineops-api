package com.mineops.mineopsapi.workforce.domain.model.valueobjects;

/**
 * Indica si un operador forma parte actualmente de la plantilla.
 */
public enum OperatorStatus {

    ACTIVE,
    INACTIVE;

    public boolean allowsAssignment() {
        return this == ACTIVE;
    }
}
