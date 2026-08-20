package com.mineops.mineopsapi.assets.application.internal.commandservices;

import com.mineops.mineopsapi.assets.domain.model.aggregates.Equipment;
import com.mineops.mineopsapi.assets.domain.model.commands.ChangeEquipmentStatusCommand;
import com.mineops.mineopsapi.assets.domain.model.commands.CreateEquipmentCommand;
import com.mineops.mineopsapi.assets.domain.model.commands.RegisterEquipmentUsageCommand;
import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;
import com.mineops.mineopsapi.assets.domain.services.EquipmentCommandService;
import com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories.EquipmentRepository;
import com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories.EquipmentTypeRepository;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceConflictException;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class EquipmentCommandServiceImpl implements EquipmentCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EquipmentCommandServiceImpl.class);

    private final EquipmentRepository equipmentRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;

    public EquipmentCommandServiceImpl(
            EquipmentRepository equipmentRepository, EquipmentTypeRepository equipmentTypeRepository) {
        this.equipmentRepository = equipmentRepository;
        this.equipmentTypeRepository = equipmentTypeRepository;
    }

    @Override
    @Transactional
    public Optional<Equipment> handle(CreateEquipmentCommand command) {
        var code = command.code() == null ? "" : command.code().trim().toUpperCase();
        if (equipmentRepository.existsByCode(code)) {
            throw new ResourceConflictException("El equipo %s ya existe".formatted(code));
        }
        var equipmentType = equipmentTypeRepository.findById(command.equipmentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("El tipo de equipo", command.equipmentTypeId()));
        var initialHourMeter = command.initialHourMeter() == null ? BigDecimal.ZERO : command.initialHourMeter();
        var equipment = new Equipment(command.code(), equipmentType, initialHourMeter);
        LOGGER.info("Agregando el equipo {} de tipo {}", code, equipmentType.getCode());
        return Optional.of(equipmentRepository.save(equipment));
    }

    /**
     * Aplica las transiciones de estado que una persona puede provocar. Liberar una máquina que
     * alcanzó su umbral no es una de ellas a propósito: la única salida de {@code BLOCKED} es
     * registrar el mantenimiento, que es lo que deja la entrada de historial.
     */
    @Override
    @Transactional
    public Optional<Equipment> handle(ChangeEquipmentStatusCommand command) {
        var equipment = findEquipment(command.equipmentId());
        switch (command.targetStatus()) {
            case IN_MAINTENANCE -> equipment.sendToWorkshop();
            case OUT_OF_SERVICE -> equipment.withdrawFromService();
            case AVAILABLE -> releaseToService(equipment);
            case BLOCKED -> throw new IllegalArgumentException(
                    "Una máquina la bloquea su horómetro, no una persona");
        }
        LOGGER.info("El equipo {} pasó a {}", equipment.getCode(), equipment.getStatus());
        return Optional.of(equipmentRepository.save(equipment));
    }

    @Override
    @Transactional
    public Optional<Equipment> handle(RegisterEquipmentUsageCommand command) {
        var equipment = findEquipment(command.equipmentId());
        var blocked = equipment.registerUsage(command.hours());
        if (blocked) {
            LOGGER.info(
                    "El equipo {} alcanzó su umbral con {} horas y quedó bloqueado",
                    equipment.getCode(),
                    equipment.getHourMeter());
        }
        return Optional.of(equipmentRepository.save(equipment));
    }

    private void releaseToService(Equipment equipment) {
        if (equipment.getStatus() == EquipmentStatus.BLOCKED
                || equipment.getStatus() == EquipmentStatus.IN_MAINTENANCE) {
            throw new IllegalArgumentException(
                    "El equipo %s solo puede liberarse registrando su mantenimiento".formatted(equipment.getCode()));
        }
        equipment.returnToService();
    }

    private Equipment findEquipment(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("El equipo", equipmentId));
    }
}
