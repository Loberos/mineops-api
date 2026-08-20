package com.mineops.mineopsapi.assets.application.internal.commandservices;

import com.mineops.mineopsapi.assets.domain.model.aggregates.MaintenanceRecord;
import com.mineops.mineopsapi.assets.domain.model.commands.RegisterMaintenanceCommand;
import com.mineops.mineopsapi.assets.domain.services.MaintenanceCommandService;
import com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories.EquipmentRepository;
import com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories.MaintenanceRecordRepository;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class MaintenanceCommandServiceImpl implements MaintenanceCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceCommandServiceImpl.class);

    private final EquipmentRepository equipmentRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public MaintenanceCommandServiceImpl(
            EquipmentRepository equipmentRepository, MaintenanceRecordRepository maintenanceRecordRepository) {
        this.equipmentRepository = equipmentRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    /**
     * Liberar la máquina y escribir su entrada de historial son una sola operación: una máquina
     * liberada sin constancia de por qué anularía el sentido del historial.
     */
    @Override
    @Transactional
    public Optional<MaintenanceRecord> handle(RegisterMaintenanceCommand command) {
        var equipment = equipmentRepository.findById(command.equipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("El equipo", command.equipmentId()));

        var performedOn = command.performedOn() == null ? LocalDate.now() : command.performedOn();
        if (performedOn.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede registrar un mantenimiento con fecha futura");
        }
        // El taller puede reportar una lectura mayor a la que conoce el sistema, porque la máquina
        // siguió trabajando entre el último turno cerrado y la parada.
        var readingAtMaintenance = command.hourMeter() == null ? equipment.getHourMeter() : command.hourMeter();

        var closedCycle = equipment.completeMaintenance(readingAtMaintenance, performedOn);
        equipmentRepository.save(equipment);

        var record = MaintenanceRecord.forCompletedMaintenance(
                equipment,
                closedCycle,
                performedOn,
                readingAtMaintenance,
                command.responsible(),
                command.observations());

        LOGGER.info(
                "Mantenimiento registrado para {} con {} horas (desfase de {} horas), siguiente umbral en {} horas",
                equipment.getCode(),
                readingAtMaintenance,
                record.getOverrunHours(),
                equipment.getMaintenanceThresholdHours());

        return Optional.of(maintenanceRecordRepository.save(record));
    }
}
