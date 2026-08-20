package com.mineops.mineopsapi.operations.domain.model.valueobjects;

/**
 * Ciclo de vida de una asignación.
 * <p>
 * {@link #AT_RISK} existe porque una programación se arma con días de anticipación y la realidad se
 * mueve por debajo. Cuando una máquina se bloquea a mitad de semana, las asignaciones ya planificadas
 * para ella no se borran en silencio: un planificador perdería el trabajo de rearmar la dotación y
 * nunca se enteraría de lo que pasó. Se marcan, y una persona decide si reasigna o cancela.
 * </p>
 */
public enum AssignmentStatus {

    /** Planificada y actualmente válida. */
    SCHEDULED,

    /** Planificada, pero algo cambió después que ahora la impediría. Necesita una decisión. */
    AT_RISK,

    /** Cancelada. Ya no ocupa al operador ni a la máquina. */
    CANCELLED,

    /** Trabajada y liquidada al cerrarse el turno. */
    COMPLETED;

    /**
     * Indica si la asignación todavía retiene al operador y a la máquina para su turno. Las canceladas
     * liberan a ambos, que es lo que permite programar un reemplazo en su lugar.
     */
    public boolean occupiesResources() {
        return this == SCHEDULED || this == AT_RISK || this == COMPLETED;
    }

    public boolean isOpen() {
        return this == SCHEDULED || this == AT_RISK;
    }
}
