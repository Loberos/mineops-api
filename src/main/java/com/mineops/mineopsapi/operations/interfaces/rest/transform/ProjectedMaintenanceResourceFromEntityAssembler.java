package com.mineops.mineopsapi.operations.interfaces.rest.transform;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.ProjectedMaintenance;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.ProjectedMaintenanceResource;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class ProjectedMaintenanceResourceFromEntityAssembler {

    private ProjectedMaintenanceResourceFromEntityAssembler() {
    }

    public static ProjectedMaintenanceResource toResourceFromEntity(ProjectedMaintenance projection) {
        var daysUntilCrossing = projection.crossingDate() == null
                ? null
                : ChronoUnit.DAYS.between(LocalDate.now(), projection.crossingDate());
        return new ProjectedMaintenanceResource(
                projection.equipmentId(),
                projection.equipmentCode(),
                projection.equipmentTypeName(),
                projection.currentHourMeter(),
                projection.thresholdHours(),
                projection.hoursUntilMaintenance(),
                projection.scheduledHoursInHorizon(),
                projection.projectedHourMeter(),
                projection.alreadyBlocked(),
                projection.willReachThreshold(),
                projection.crossingDate(),
                projection.crossingJourney() == null ? null : projection.crossingJourney().name(),
                projection.crossingShiftId(),
                projection.hourMeterAtCrossing(),
                projection.scheduledShiftsInHorizon(),
                daysUntilCrossing);
    }
}
