package com.mineops.mineopsapi.assets.application.internal.commandservices;

import com.mineops.mineopsapi.assets.domain.model.aggregates.EquipmentType;
import com.mineops.mineopsapi.assets.domain.model.commands.CreateEquipmentTypeCommand;
import com.mineops.mineopsapi.assets.domain.model.commands.UpdateEquipmentTypeCommand;
import com.mineops.mineopsapi.assets.domain.services.EquipmentTypeCommandService;
import com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories.EquipmentTypeRepository;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceConflictException;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EquipmentTypeCommandServiceImpl implements EquipmentTypeCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EquipmentTypeCommandServiceImpl.class);

    private final EquipmentTypeRepository equipmentTypeRepository;

    public EquipmentTypeCommandServiceImpl(EquipmentTypeRepository equipmentTypeRepository) {
        this.equipmentTypeRepository = equipmentTypeRepository;
    }

    @Override
    @Transactional
    public Optional<EquipmentType> handle(CreateEquipmentTypeCommand command) {
        var code = command.code() == null ? "" : command.code().trim().toUpperCase();
        if (equipmentTypeRepository.existsByCode(code)) {
            throw new ResourceConflictException("Ya existe un tipo de equipo con el código %s".formatted(code));
        }
        var equipmentType = new EquipmentType(
                command.code(), command.name(), command.maintenanceIntervalHours(), command.description());
        LOGGER.info("Agregando el tipo de equipo {} con un intervalo de {} horas",
                code, command.maintenanceIntervalHours());
        return Optional.of(equipmentTypeRepository.save(equipmentType));
    }

    @Override
    @Transactional
    public Optional<EquipmentType> handle(UpdateEquipmentTypeCommand command) {
        var equipmentType = equipmentTypeRepository.findById(command.equipmentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("El tipo de equipo", command.equipmentTypeId()));
        equipmentType.update(command.name(), command.maintenanceIntervalHours(), command.description());
        return Optional.of(equipmentTypeRepository.save(equipmentType));
    }
}
