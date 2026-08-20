package com.mineops.mineopsapi.assets.interfaces.rest.resources;

import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeEquipmentStatusResource(
        @NotNull(message = "El estado destino es obligatorio") EquipmentStatus status) {
}
