package com.mineops.mineopsapi.operations.domain.model.valueobjects;

import java.time.LocalTime;

/**
 * Las dos jornadas en que se divide un día de operación minera.
 * <p>
 * Cada una lleva la hora a la que empieza, que es lo que convierte a un turno en un intervalo real
 * del calendario. Eso importa más allá de la presentación: un turno de noche cruza la medianoche, así
 * que una certificación que vence al final de ese día no lo cubre completo.
 * </p>
 */
public enum Journey {

    DAY(LocalTime.of(7, 0)),
    NIGHT(LocalTime.of(19, 0));

    private final LocalTime startsAt;

    Journey(LocalTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalTime startsAt() {
        return startsAt;
    }
}
