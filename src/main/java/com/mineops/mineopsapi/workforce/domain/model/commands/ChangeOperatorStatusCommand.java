package com.mineops.mineopsapi.workforce.domain.model.commands;

import com.mineops.mineopsapi.workforce.domain.model.valueobjects.OperatorStatus;

public record ChangeOperatorStatusCommand(Long operatorId, OperatorStatus targetStatus) {
}
