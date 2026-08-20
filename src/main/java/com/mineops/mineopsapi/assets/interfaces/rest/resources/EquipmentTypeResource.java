package com.mineops.mineopsapi.assets.interfaces.rest.resources;

import java.math.BigDecimal;

public record EquipmentTypeResource(
        Long id,
        String code,
        String name,
        BigDecimal maintenanceIntervalHours,
        String description,
        boolean active) {
}
