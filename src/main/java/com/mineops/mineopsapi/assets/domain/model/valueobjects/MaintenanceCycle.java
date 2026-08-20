package com.mineops.mineopsapi.assets.domain.model.valueobjects;

import java.math.BigDecimal;

/**
 * Ciclo de mantenimiento que un equipo está corriendo actualmente: cuántas horas de uso tiene entre
 * paradas, y la lectura de horómetro a la que vence la parada vigente.
 * <p>
 * <strong>Cómo se calcula el siguiente umbral.</strong> El mantenimiento rara vez se hace exactamente
 * en el umbral, y la regla ingenua <em>siguiente = lectura real + intervalo</em> deja que ese atraso
 * se acumule: atendido 30 horas tarde, dos veces, y la máquina termina operando 60 horas más allá de
 * lo que el fabricante previó. Por eso este ciclo ancla el siguiente umbral al umbral
 * <em>planificado</em> cuando el mantenimiento ocurrió en o después de él, de modo que el atraso se
 * absorbe en lugar de arrastrarse. Cuando el mantenimiento se hace antes de tiempo el ancla es la
 * lectura real, para no castigar con un ciclo más corto a quien atiende la máquina anticipadamente.
 * </p>
 *
 * @param intervalHours  horas de uso que otorga un ciclo
 * @param thresholdHours lectura de horómetro a la que vence el ciclo vigente
 */
public record MaintenanceCycle(BigDecimal intervalHours, BigDecimal thresholdHours) {

    public MaintenanceCycle {
        if (intervalHours == null || intervalHours.signum() <= 0) {
            throw new IllegalArgumentException("El intervalo de mantenimiento debe ser mayor que cero");
        }
        if (thresholdHours == null || thresholdHours.signum() < 0) {
            throw new IllegalArgumentException("El umbral de mantenimiento no puede ser negativo");
        }
    }

    /**
     * Primer ciclo de una máquina que entra a la flota con el horómetro indicado.
     */
    public static MaintenanceCycle startingAt(BigDecimal intervalHours, BigDecimal currentHourMeter) {
        return new MaintenanceCycle(intervalHours, currentHourMeter.add(intervalHours));
    }

    /**
     * Ciclo que sigue a un mantenimiento realizado con la lectura de horómetro indicada.
     *
     * @param readingAtMaintenance horómetro registrado al atender la máquina
     * @return el siguiente ciclo, siempre con vencimiento estrictamente posterior a la lectura
     */
    public MaintenanceCycle nextAfterMaintenanceAt(BigDecimal readingAtMaintenance) {
        if (readingAtMaintenance.compareTo(thresholdHours) < 0) {
            // Atendido antes de tiempo: se reinicia la cuenta desde la lectura real.
            return new MaintenanceCycle(intervalHours, readingAtMaintenance.add(intervalHours));
        }
        // Atendido a tiempo o tarde: se conserva el ritmo planificado para no arrastrar el atraso.
        var nextThreshold = thresholdHours.add(intervalHours);
        // Un atraso mayor a un ciclo completo dejaría a la máquina vencida apenas sale del taller.
        while (nextThreshold.compareTo(readingAtMaintenance) <= 0) {
            nextThreshold = nextThreshold.add(intervalHours);
        }
        return new MaintenanceCycle(intervalHours, nextThreshold);
    }

    /**
     * Cuántas horas operó la máquina más allá de su umbral antes de ser atendida. Nunca es negativo.
     */
    public BigDecimal overrunAt(BigDecimal readingAtMaintenance) {
        var overrun = readingAtMaintenance.subtract(thresholdHours);
        return overrun.signum() < 0 ? BigDecimal.ZERO : overrun;
    }

    /**
     * Horas de uso que quedan antes de alcanzar el umbral. Negativo una vez que se excedió.
     */
    public BigDecimal remainingAt(BigDecimal currentHourMeter) {
        return thresholdHours.subtract(currentHourMeter);
    }

    public boolean isReachedAt(BigDecimal currentHourMeter) {
        return currentHourMeter.compareTo(thresholdHours) >= 0;
    }
}
