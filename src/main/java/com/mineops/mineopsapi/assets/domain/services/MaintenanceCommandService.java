package com.mineops.mineopsapi.assets.domain.services;

import com.mineops.mineopsapi.assets.domain.model.aggregates.MaintenanceRecord;
import com.mineops.mineopsapi.assets.domain.model.commands.RegisterMaintenanceCommand;

import java.util.Optional;

public interface MaintenanceCommandService {

    /**
     * Libera la máquina y escribe la entrada de historial, en una sola transacción.
     */
    Optional<MaintenanceRecord> handle(RegisterMaintenanceCommand command);
}
