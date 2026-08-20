package com.mineops.mineopsapi.operations.interfaces.rest.transform;

import com.mineops.mineopsapi.operations.domain.model.commands.CloseShiftCommand;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.CloseShiftResource;

import java.util.List;

public final class CloseShiftCommandFromResourceAssembler {

    private CloseShiftCommandFromResourceAssembler() {
    }

    public static CloseShiftCommand toCommandFromResource(Long shiftId, CloseShiftResource resource) {
        var closures = resource == null || resource.closures() == null
                ? List.<CloseShiftCommand.AssignmentClosure>of()
                : resource.closures().stream()
                        .map(closure -> new CloseShiftCommand.AssignmentClosure(
                                closure.assignmentId(), closure.workedHours(), closure.note()))
                        .toList();
        return new CloseShiftCommand(shiftId, closures);
    }
}
