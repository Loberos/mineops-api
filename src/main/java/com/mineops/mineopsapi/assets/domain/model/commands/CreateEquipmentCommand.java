package com.mineops.mineopsapi.assets.domain.model.commands;

import java.math.BigDecimal;

/**
 * Registra una máquina en la flota.
 *
 * @param code             código único pintado en la máquina, por ejemplo {@code CAM-001}
 * @param equipmentTypeId  familia a la que pertenece la máquina
 * @param initialHourMeter horas ya acumuladas al ingresar a la flota
 */
public record CreateEquipmentCommand(String code, Long equipmentTypeId, BigDecimal initialHourMeter) {
}
