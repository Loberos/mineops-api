package com.mineops.mineopsapi.operations.domain.model.commands;

import java.math.BigDecimal;

public record UpdateShiftPlanCommand(Long shiftId, BigDecimal plannedHours, String notes) {
}
