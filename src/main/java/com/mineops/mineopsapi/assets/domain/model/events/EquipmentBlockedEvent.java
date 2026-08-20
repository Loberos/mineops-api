package com.mineops.mineopsapi.assets.domain.model.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Se publica cuando un equipo alcanza su umbral de mantenimiento y queda fuera de servicio.
 * <p>
 * Este es el lenguaje publicado entre el contexto de activos y el resto del sistema: el contexto de
 * operaciones se suscribe para marcar los turnos que ya estaban programados con esa máquina, sin que
 * el contexto de activos necesite siquiera saber que existen los turnos.
 * </p>
 *
 * @param equipmentId    identificador de la máquina bloqueada
 * @param equipmentCode  código de la máquina, para mensajes que no deban hacer una segunda consulta
 * @param hourMeter      lectura que cruzó el umbral
 * @param thresholdHours umbral que se cruzó
 * @param occurredAt     momento en que se aplicó el bloqueo
 */
public record EquipmentBlockedEvent(
        Long equipmentId,
        String equipmentCode,
        BigDecimal hourMeter,
        BigDecimal thresholdHours,
        LocalDateTime occurredAt) {

    public static EquipmentBlockedEvent of(
            Long equipmentId, String equipmentCode, BigDecimal hourMeter, BigDecimal thresholdHours) {
        return new EquipmentBlockedEvent(
                equipmentId, equipmentCode, hourMeter, thresholdHours, LocalDateTime.now());
    }
}
