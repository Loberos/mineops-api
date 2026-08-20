package com.mineops.mineopsapi.assets.domain.model.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Se publica cuando una máquina vuelve a servicio después de ser atendida.
 * <p>
 * Es la contraparte de {@link EquipmentBlockedEvent}: los turnos que se marcaron cuando la máquina
 * se detuvo pueden desmarcarse ahora que puede volver a trabajar, sin que un planificador tenga que
 * recorrer el calendario buscándolos.
 * </p>
 *
 * @param equipmentId        identificador de la máquina
 * @param equipmentCode      código de la máquina
 * @param hourMeter          lectura con la que vuelve a servicio
 * @param nextThresholdHours umbral del ciclo que se acaba de abrir
 * @param occurredAt         momento en que se liberó
 */
public record EquipmentReleasedEvent(
        Long equipmentId,
        String equipmentCode,
        BigDecimal hourMeter,
        BigDecimal nextThresholdHours,
        LocalDateTime occurredAt) {

    public static EquipmentReleasedEvent of(
            Long equipmentId, String equipmentCode, BigDecimal hourMeter, BigDecimal nextThresholdHours) {
        return new EquipmentReleasedEvent(
                equipmentId, equipmentCode, hourMeter, nextThresholdHours, LocalDateTime.now());
    }
}
