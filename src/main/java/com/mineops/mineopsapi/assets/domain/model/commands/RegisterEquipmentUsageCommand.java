package com.mineops.mineopsapi.assets.domain.model.commands;

import java.math.BigDecimal;

/**
 * Suma horas trabajadas al horómetro de una máquina, lo que puede bloquearla.
 * <p>
 * Lo emite el contexto de operaciones al cerrar un turno; es la única vía por la que el uso entra a
 * la flota.
 * </p>
 *
 * @param equipmentId la máquina que trabajó
 * @param hours       horas efectivamente trabajadas, nunca negativas
 */
public record RegisterEquipmentUsageCommand(Long equipmentId, BigDecimal hours) {
}
