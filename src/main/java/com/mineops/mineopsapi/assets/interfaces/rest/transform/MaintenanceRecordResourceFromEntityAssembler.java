package com.mineops.mineopsapi.assets.interfaces.rest.transform;

import com.mineops.mineopsapi.assets.domain.model.aggregates.MaintenanceRecord;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.MaintenanceRecordResource;

public final class MaintenanceRecordResourceFromEntityAssembler {

    private MaintenanceRecordResourceFromEntityAssembler() {
    }

    public static MaintenanceRecordResource toResourceFromEntity(MaintenanceRecord entity) {
        return new MaintenanceRecordResource(
                entity.getId(),
                entity.getEquipmentId(),
                entity.getEquipmentCode(),
                entity.getPerformedOn(),
                entity.getHourMeter(),
                entity.getThresholdHours(),
                entity.getOverrunHours(),
                entity.getNextThresholdHours(),
                entity.getResponsible(),
                entity.getObservations(),
                entity.wasOverdue());
    }
}
