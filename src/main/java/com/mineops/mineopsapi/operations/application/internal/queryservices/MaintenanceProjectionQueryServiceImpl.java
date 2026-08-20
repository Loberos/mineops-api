package com.mineops.mineopsapi.operations.application.internal.queryservices;

import com.mineops.mineopsapi.assets.interfaces.acl.EquipmentContextFacade;
import com.mineops.mineopsapi.assets.interfaces.acl.EquipmentSnapshot;
import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.operations.domain.model.queries.GetMaintenanceProjectionQuery;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.Journey;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.ProjectedMaintenance;
import com.mineops.mineopsapi.operations.domain.services.MaintenanceProjectionQueryService;
import com.mineops.mineopsapi.operations.infrastructure.persistence.jpa.repositories.ShiftRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regla de negocio 12: qué máquinas alcanzarán su mantenimiento dentro del horizonte.
 * <p>
 * <strong>Por qué esto no puede responderse mirando el estado presente.</strong> Un camión al que le
 * faltan 40 horas para su umbral se ve perfectamente sano hoy, y tiene tres turnos de doce horas esta
 * semana. La respuesta solo aparece reproduciendo la programación: los turnos se recorren en orden
 * cronológico, cada máquina lleva un horómetro corriendo, y el momento en que ese acumulado cruza el
 * umbral es el turno en que la máquina se va a detener. Sumar sin más las horas de la semana diría
 * <em>si</em> ocurre pero no <em>cuándo</em>, y el cuándo es justamente lo que un planificador
 * necesita para reservar taller.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class MaintenanceProjectionQueryServiceImpl implements MaintenanceProjectionQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceProjectionQueryServiceImpl.class);

    private final ShiftRepository shiftRepository;
    private final EquipmentContextFacade equipmentContextFacade;

    public MaintenanceProjectionQueryServiceImpl(
            ShiftRepository shiftRepository, EquipmentContextFacade equipmentContextFacade) {
        this.shiftRepository = shiftRepository;
        this.equipmentContextFacade = equipmentContextFacade;
    }

    @Override
    public List<ProjectedMaintenance> handle(GetMaintenanceProjectionQuery query) {
        var today = LocalDate.now();
        var horizonEnd = today.plusDays(query.horizonDays());
        var shifts = shiftRepository.findPlannedWithAssignmentsInRange(today, horizonEnd);

        var fleet = indexFleetById();
        var projections = replaySchedule(shifts, fleet);

        // Una máquina que ya está detenida pertenece a esta lista aunque no tenga nada programado: no
        // es que vaya a alcanzar el mantenimiento, es que ya lo alcanzó.
        fleet.values().stream()
                .filter(equipment -> !equipment.isAvailableForAssignment())
                .filter(equipment -> !projections.containsKey(equipment.id()))
                .forEach(equipment -> projections.put(equipment.id(), new ProjectionState(equipment)));

        var result = projections.values().stream()
                .filter(ProjectionState::deservesAttention)
                .map(ProjectionState::toProjection)
                .sorted(byUrgency())
                .toList();

        LOGGER.debug(
                "Proyección de mantenimiento a {} días: {} turnos recorridos, {} máquinas señaladas",
                query.horizonDays(),
                shifts.size(),
                result.size());
        return result;
    }

    /**
     * Recorre la programación hacia adelante, acumulando horas sobre una copia del horómetro de cada
     * máquina.
     */
    private Map<Long, ProjectionState> replaySchedule(List<Shift> shifts, Map<Long, EquipmentSnapshot> fleet) {
        Map<Long, ProjectionState> states = new LinkedHashMap<>();
        for (var shift : shifts) {
            for (var assignment : shift.activeAssignments()) {
                var equipment = fleet.get(assignment.getEquipmentId());
                if (equipment == null) {
                    // La máquina se retiró de la flota después de haberse planificado el turno.
                    continue;
                }
                states.computeIfAbsent(equipment.id(), id -> new ProjectionState(equipment))
                        .addShift(shift);
            }
        }
        return states;
    }

    private Map<Long, EquipmentSnapshot> indexFleetById() {
        Map<Long, EquipmentSnapshot> fleet = new LinkedHashMap<>();
        equipmentContextFacade.fetchAllEquipment().forEach(equipment -> fleet.put(equipment.id(), equipment));
        return fleet;
    }

    /**
     * Primero las máquinas ya detenidas, luego las que se detienen antes, y luego las que tienen menos
     * margen restante.
     */
    private static Comparator<ProjectedMaintenance> byUrgency() {
        return Comparator
                .comparing(ProjectedMaintenance::alreadyBlocked, Comparator.reverseOrder())
                .thenComparing(
                        ProjectedMaintenance::crossingDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProjectedMaintenance::hoursUntilMaintenance);
    }

    /**
     * Estado corriente de una máquina mientras se reproduce la programación.
     */
    private static final class ProjectionState {

        private final EquipmentSnapshot equipment;
        private BigDecimal runningHourMeter;
        private BigDecimal scheduledHours = BigDecimal.ZERO;
        private int scheduledShifts;

        private LocalDate crossingDate;
        private Journey crossingJourney;
        private Long crossingShiftId;
        private BigDecimal hourMeterAtCrossing;

        private ProjectionState(EquipmentSnapshot equipment) {
            this.equipment = equipment;
            this.runningHourMeter = equipment.hourMeter();
        }

        private void addShift(Shift shift) {
            var plannedHours = shift.getPlannedHours();
            scheduledHours = scheduledHours.add(plannedHours);
            scheduledShifts++;
            runningHourMeter = runningHourMeter.add(plannedHours);

            var reachesThreshold =
                    runningHourMeter.compareTo(equipment.maintenanceThresholdHours()) >= 0;
            if (reachesThreshold && crossingDate == null) {
                crossingDate = shift.getDate();
                crossingJourney = shift.getJourney();
                crossingShiftId = shift.getId();
                hourMeterAtCrossing = runningHourMeter;
            }
        }

        private boolean deservesAttention() {
            return crossingDate != null || !equipment.isAvailableForAssignment();
        }

        private ProjectedMaintenance toProjection() {
            return new ProjectedMaintenance(
                    equipment.id(),
                    equipment.code(),
                    equipment.equipmentTypeName(),
                    equipment.hourMeter(),
                    equipment.maintenanceThresholdHours(),
                    equipment.hoursUntilMaintenance(),
                    scheduledHours,
                    runningHourMeter,
                    !equipment.isAvailableForAssignment(),
                    crossingDate != null,
                    crossingDate,
                    crossingJourney,
                    crossingShiftId,
                    hourMeterAtCrossing,
                    scheduledShifts);
        }
    }
}
