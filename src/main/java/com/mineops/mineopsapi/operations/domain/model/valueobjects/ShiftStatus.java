package com.mineops.mineopsapi.operations.domain.model.valueobjects;

/**
 * Ciclo de vida de un turno.
 */
public enum ShiftStatus {

    /** Programado y todavía abierto a cambios. */
    PLANNED,

    /** Trabajado y liquidado: sus horas ya se sumaron a los horómetros y nada más puede cambiar. */
    CLOSED,

    /** Suspendido antes de haberse trabajado. */
    CANCELLED;

    public boolean acceptsAssignments() {
        return this == PLANNED;
    }
}
