package com.mineops.mineopsapi.workforce.interfaces.rest.resources;

import com.mineops.mineopsapi.workforce.domain.model.valueobjects.OperatorStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeOperatorStatusResource(
        @NotNull(message = "El estado destino es obligatorio") OperatorStatus status) {
}
