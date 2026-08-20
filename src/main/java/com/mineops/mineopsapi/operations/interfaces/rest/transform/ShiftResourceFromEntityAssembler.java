package com.mineops.mineopsapi.operations.interfaces.rest.transform;

import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentStatus;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.ShiftResource;

import java.util.Comparator;

public final class ShiftResourceFromEntityAssembler {

    private ShiftResourceFromEntityAssembler() {
    }

    public static ShiftResource toResourceFromEntity(Shift entity) {
        var assignments = entity.getAssignments().stream()
                .sorted(Comparator.comparing(assignment -> assignment.getEquipmentCode()))
                .map(AssignmentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        var atRisk = (int) entity.getAssignments().stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.AT_RISK)
                .count();
        return new ShiftResource(
                entity.getId(),
                entity.getDate(),
                entity.getJourney().name(),
                entity.getPlannedHours(),
                entity.getStatus().name(),
                entity.startsAt(),
                entity.endsAt(),
                entity.endDate(),
                entity.getClosedAt(),
                entity.getNotes(),
                entity.activeAssignments().size(),
                atRisk,
                assignments);
    }
}
