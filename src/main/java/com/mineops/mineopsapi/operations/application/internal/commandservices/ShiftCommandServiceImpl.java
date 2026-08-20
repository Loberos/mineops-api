package com.mineops.mineopsapi.operations.application.internal.commandservices;

import com.mineops.mineopsapi.assets.interfaces.acl.EquipmentContextFacade;
import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.operations.domain.model.commands.CancelShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.CloseShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.CreateShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.UpdateShiftPlanCommand;
import com.mineops.mineopsapi.operations.domain.model.entities.Assignment;
import com.mineops.mineopsapi.operations.domain.services.ShiftCommandService;
import com.mineops.mineopsapi.operations.infrastructure.configuration.OperationsProperties;
import com.mineops.mineopsapi.operations.infrastructure.persistence.jpa.repositories.ShiftRepository;
import com.mineops.mineopsapi.shared.domain.exceptions.BusinessRuleViolationException;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceConflictException;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ShiftCommandServiceImpl implements ShiftCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShiftCommandServiceImpl.class);

    /** Ningún turno puede durar plausiblemente más de un día completo. */
    private static final BigDecimal MAX_WORKED_HOURS = BigDecimal.valueOf(24);

    private static final String RULE_HOURS_OUT_OF_RANGE = "WORKED_HOURS_OUT_OF_RANGE";
    private static final String RULE_DEVIATION_NEEDS_JUSTIFICATION = "HOURS_DEVIATION_NEEDS_JUSTIFICATION";

    private final ShiftRepository shiftRepository;
    private final EquipmentContextFacade equipmentContextFacade;
    private final OperationsProperties operationsProperties;

    public ShiftCommandServiceImpl(
            ShiftRepository shiftRepository,
            EquipmentContextFacade equipmentContextFacade,
            OperationsProperties operationsProperties) {
        this.shiftRepository = shiftRepository;
        this.equipmentContextFacade = equipmentContextFacade;
        this.operationsProperties = operationsProperties;
    }

    @Override
    @Transactional
    public Optional<Shift> handle(CreateShiftCommand command) {
        if (shiftRepository.existsByDateAndJourney(command.date(), command.journey())) {
            throw new ResourceConflictException(
                    "El turno %s del %s ya existe".formatted(command.journey(), command.date()));
        }
        var shift = new Shift(command.date(), command.journey(), command.plannedHours(), command.notes());
        LOGGER.info("Programando el turno {} del {}", command.journey(), command.date());
        return Optional.of(shiftRepository.save(shift));
    }

    @Override
    @Transactional
    public Optional<Shift> handle(UpdateShiftPlanCommand command) {
        var shift = findShift(command.shiftId());
        shift.updatePlan(command.plannedHours(), command.notes());
        return Optional.of(shiftRepository.save(shift));
    }

    @Override
    @Transactional
    public Optional<Shift> handle(CancelShiftCommand command) {
        var shift = findShift(command.shiftId());
        shift.cancel(command.reason());
        LOGGER.info("Turno {} cancelado: {}", shift.getId(), command.reason());
        return Optional.of(shiftRepository.save(shift));
    }

    /**
     * Liquida el turno.
     * <p>
     * <strong>Horas que difieren del plan.</strong> Manda lo que se trabajó: esas horas se toman como
     * vienen y se empujan a los horómetros. Un turno de ocho horas que se extendió a diez puso diez
     * horas sobre la máquina, y fingir otra cosa corrompería el cronograma de mantenimiento del que
     * depende todo lo demás. Lo que el sistema sí exige es que una desviación material se explique por
     * escrito, para que la diferencia quede visible en vez de disolverse entre los números. Por debajo
     * de la tolerancia configurada la diferencia se trata como variación normal y no requiere
     * justificación.
     * </p>
     */
    @Override
    @Transactional
    public Optional<Shift> handle(CloseShiftCommand command) {
        var shift = findShift(command.shiftId());
        if (!shift.isOpen()) {
            throw new ResourceConflictException("El turno %s ya fue liquidado".formatted(shift.getId()));
        }

        var closuresByAssignment = indexClosures(command);
        var openAssignments = shift.openAssignments();

        // Todos los problemas del cierre se reúnen antes de escribir nada, para informarlos juntos en
        // vez de corregirlos de a uno por viaje.
        var violations = new ArrayList<BusinessRuleViolation>();
        var settlements = new ArrayList<Settlement>();

        for (var assignment : openAssignments) {
            var closure = closuresByAssignment.get(assignment.getId());
            var workedHours = closure == null || closure.workedHours() == null
                    ? shift.getPlannedHours()
                    : closure.workedHours();
            var note = closure == null ? null : closure.note();

            if (workedHours.signum() < 0 || workedHours.compareTo(MAX_WORKED_HOURS) > 0) {
                violations.add(BusinessRuleViolation.blocking(
                        RULE_HOURS_OUT_OF_RANGE,
                        "Las horas trabajadas por %s en %s deben estar entre 0 y %s"
                                .formatted(assignment.getOperatorName(), assignment.getEquipmentCode(),
                                        MAX_WORKED_HOURS)));
                continue;
            }

            if (deviatesBeyondTolerance(shift.getPlannedHours(), workedHours) && isBlank(note)) {
                violations.add(BusinessRuleViolation.blocking(
                        RULE_DEVIATION_NEEDS_JUSTIFICATION,
                        "%s trabajó %s horas en %s frente a %s planificadas. Una diferencia mayor al %s%% necesita un motivo por escrito."
                                .formatted(
                                        assignment.getOperatorName(),
                                        workedHours,
                                        assignment.getEquipmentCode(),
                                        shift.getPlannedHours(),
                                        tolerancePercentage())));
                continue;
            }

            settlements.add(new Settlement(assignment, workedHours, note));
        }

        if (!violations.isEmpty()) {
            throw new BusinessRuleViolationException("El turno no puede cerrarse tal como se envió", violations);
        }

        settlements.forEach(settlement -> {
            settlement.assignment().complete(settlement.workedHours(), settlement.note());
            // Sumar las horas es lo que puede llevar una máquina más allá de su umbral y bloquearla. Esa
            // decisión pertenece al contexto de activos; este contexto solo informa lo que se trabajó.
            var equipment = equipmentContextFacade.registerUsage(
                    settlement.assignment().getEquipmentId(), settlement.workedHours());
            LOGGER.info(
                    "Turno {}: se sumaron {} horas a {}, horómetro ahora en {}",
                    shift.getId(),
                    settlement.workedHours(),
                    equipment.code(),
                    equipment.hourMeter());
        });

        shift.close();
        LOGGER.info("Turno {} ({} {}) cerrado con {} asignaciones liquidadas",
                shift.getId(), shift.getDate(), shift.getJourney(), settlements.size());
        return Optional.of(shiftRepository.save(shift));
    }

    private Map<Long, CloseShiftCommand.AssignmentClosure> indexClosures(CloseShiftCommand command) {
        if (command.closures() == null) {
            return Map.of();
        }
        return command.closures().stream()
                .collect(Collectors.toMap(
                        CloseShiftCommand.AssignmentClosure::assignmentId,
                        Function.identity(),
                        (first, second) -> second));
    }

    private boolean deviatesBeyondTolerance(BigDecimal plannedHours, BigDecimal workedHours) {
        var allowed = plannedHours.multiply(operationsProperties.shiftClosureTolerance());
        return workedHours.subtract(plannedHours).abs().compareTo(allowed) > 0;
    }

    private BigDecimal tolerancePercentage() {
        return operationsProperties.shiftClosureTolerance().multiply(BigDecimal.valueOf(100)).stripTrailingZeros();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Shift findShift(Long shiftId) {
        return shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("El turno", shiftId));
    }

    /**
     * Una asignación cuyo cierre pasó la validación y está lista para aplicarse.
     */
    private record Settlement(Assignment assignment, BigDecimal workedHours, String note) {
    }
}
