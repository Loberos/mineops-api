package com.mineops.mineopsapi.assets.domain.model.commands;

import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;

/**
 * Mueve una máquina entre los estados que controla una persona.
 * <p>
 * {@code BLOCKED} no es alcanzable por este comando: es el horómetro, no una persona, quien decide
 * cuándo una máquina tiene que detenerse.
 * </p>
 *
 * @param equipmentId  la máquina
 * @param targetStatus {@code IN_MAINTENANCE}, {@code OUT_OF_SERVICE} o {@code AVAILABLE}
 */
public record ChangeEquipmentStatusCommand(Long equipmentId, EquipmentStatus targetStatus) {
}
