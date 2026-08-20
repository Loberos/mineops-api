package com.mineops.mineopsapi.assets.domain.model.valueobjects;

/**
 * Estado operativo de un equipo.
 * <p>
 * Solo un equipo {@link #AVAILABLE} puede asignarse a un turno. La distinción entre {@link #BLOCKED}
 * e {@link #IN_MAINTENANCE} importa en la operación: el primero es una máquina que alcanzó su umbral
 * de horómetro y espera en el patio, el segundo es una máquina que ya está en el taller.
 * </p>
 */
public enum EquipmentStatus {

    /** Listo para programarse. */
    AVAILABLE,

    /** Alcanzó su umbral de mantenimiento. No puede asignarse hasta registrar el mantenimiento. */
    BLOCKED,

    /** Actualmente en el taller. */
    IN_MAINTENANCE,

    /** Retirado de la operación por razones ajenas al ciclo de mantenimiento. */
    OUT_OF_SERVICE;

    public boolean allowsAssignment() {
        return this == AVAILABLE;
    }
}
