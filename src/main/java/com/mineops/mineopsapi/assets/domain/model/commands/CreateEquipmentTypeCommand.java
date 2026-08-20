package com.mineops.mineopsapi.assets.domain.model.commands;

import java.math.BigDecimal;

/**
 * Agrega una familia de máquinas al catálogo.
 *
 * @param code                     código corto y único, por ejemplo {@code HAUL_TRUCK}
 * @param name                     nombre visible
 * @param maintenanceIntervalHours horas de uso que se otorgan entre paradas de mantenimiento
 * @param description              notas opcionales
 */
public record CreateEquipmentTypeCommand(
        String code, String name, BigDecimal maintenanceIntervalHours, String description) {
}
