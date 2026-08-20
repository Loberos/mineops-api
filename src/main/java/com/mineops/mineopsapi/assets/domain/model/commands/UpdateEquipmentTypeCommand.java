package com.mineops.mineopsapi.assets.domain.model.commands;

import java.math.BigDecimal;

/**
 * Actualiza una familia de máquinas. Cambiar el intervalo solo afecta a los ciclos que se abran de
 * aquí en adelante: los umbrales ya asignados a cada máquina se dejan intactos para que una edición
 * del catálogo nunca bloquee ni libere en silencio a una máquina que ya está trabajando.
 *
 * @param equipmentTypeId          el tipo a actualizar
 * @param name                     nuevo nombre visible
 * @param maintenanceIntervalHours nuevo intervalo
 * @param description              nuevas notas
 */
public record UpdateEquipmentTypeCommand(
        Long equipmentTypeId, String name, BigDecimal maintenanceIntervalHours, String description) {
}
