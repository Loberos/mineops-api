package com.mineops.mineopsapi.assets.domain.services;

import com.mineops.mineopsapi.assets.domain.model.aggregates.Equipment;
import com.mineops.mineopsapi.assets.domain.model.commands.ChangeEquipmentStatusCommand;
import com.mineops.mineopsapi.assets.domain.model.commands.CreateEquipmentCommand;
import com.mineops.mineopsapi.assets.domain.model.commands.RegisterEquipmentUsageCommand;

import java.util.Optional;

public interface EquipmentCommandService {

    Optional<Equipment> handle(CreateEquipmentCommand command);

    Optional<Equipment> handle(ChangeEquipmentStatusCommand command);

    /**
     * Suma horas trabajadas a una máquina.
     *
     * @return la máquina actualizada, para que quien llama pueda saber si el uso la bloqueó
     */
    Optional<Equipment> handle(RegisterEquipmentUsageCommand command);
}
