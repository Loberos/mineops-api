package com.mineops.mineopsapi.assets.domain.services;

import com.mineops.mineopsapi.assets.domain.model.aggregates.EquipmentType;
import com.mineops.mineopsapi.assets.domain.model.commands.CreateEquipmentTypeCommand;
import com.mineops.mineopsapi.assets.domain.model.commands.UpdateEquipmentTypeCommand;

import java.util.Optional;

public interface EquipmentTypeCommandService {

    Optional<EquipmentType> handle(CreateEquipmentTypeCommand command);

    Optional<EquipmentType> handle(UpdateEquipmentTypeCommand command);
}
