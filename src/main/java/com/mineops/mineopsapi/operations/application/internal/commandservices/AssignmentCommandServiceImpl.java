package com.mineops.mineopsapi.operations.application.internal.commandservices;

import com.mineops.mineopsapi.assets.interfaces.acl.EquipmentContextFacade;
import com.mineops.mineopsapi.iam.interfaces.acl.UserContextFacade;
import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.operations.domain.model.commands.AssignOperatorToShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.CancelAssignmentCommand;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentEvaluation;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentOutcome;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.SupervisorAuthorization;
import com.mineops.mineopsapi.operations.domain.services.AssignmentCommandService;
import com.mineops.mineopsapi.operations.domain.services.AssignmentRuleEvaluator;
import com.mineops.mineopsapi.operations.infrastructure.persistence.jpa.repositories.ShiftRepository;
import com.mineops.mineopsapi.shared.domain.exceptions.BusinessRuleViolationException;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import com.mineops.mineopsapi.workforce.interfaces.acl.OperatorContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Asigna operadores a turnos.
 * <p>
 * <strong>Cómo se mantienen separados dos supervisores que compiten por la misma máquina.</strong>
 * Tres capas, a propósito. El agregado de turno se carga y se escribe completo, así que la
 * verificación y la escritura ocurren sobre una misma foto consistente. Su columna {@code @Version}
 * hace que el segundo que escribe falle en lugar de sobrescribir al primero. Y un índice único
 * parcial en la base de datos —un operador y una máquina por turno, ignorando las filas canceladas—
 * es la garantía que sobrevive incluso a un error en las capas de arriba. A quien pierde se le pide
 * refrescar; no se le descarta en silencio.
 * </p>
 */
@Service
public class AssignmentCommandServiceImpl implements AssignmentCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentCommandServiceImpl.class);

    private final ShiftRepository shiftRepository;
    private final AssignmentRuleEvaluator ruleEvaluator;
    private final OperatorContextFacade operatorContextFacade;
    private final EquipmentContextFacade equipmentContextFacade;
    private final UserContextFacade userContextFacade;

    public AssignmentCommandServiceImpl(
            ShiftRepository shiftRepository,
            AssignmentRuleEvaluator ruleEvaluator,
            OperatorContextFacade operatorContextFacade,
            EquipmentContextFacade equipmentContextFacade,
            UserContextFacade userContextFacade) {
        this.shiftRepository = shiftRepository;
        this.ruleEvaluator = ruleEvaluator;
        this.operatorContextFacade = operatorContextFacade;
        this.equipmentContextFacade = equipmentContextFacade;
        this.userContextFacade = userContextFacade;
    }

    @Override
    @Transactional
    public Optional<AssignmentOutcome> handle(AssignOperatorToShiftCommand command) {
        var shift = findShift(command.shiftId());
        var operator = operatorContextFacade.fetchOperatorById(command.operatorId())
                .orElseThrow(() -> new ResourceNotFoundException("El operador", command.operatorId()));
        var equipment = equipmentContextFacade.fetchEquipmentById(command.equipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("El equipo", command.equipmentId()));

        var evaluation = ruleEvaluator.evaluate(new AssignmentContext(shift, operator, equipment));
        var authorization = resolveAuthorization(command, evaluation, shift);

        var assignment = shift.assign(
                operator.id(),
                operator.fullName(),
                operator.documentNumber(),
                equipment.id(),
                equipment.code(),
                equipment.equipmentTypeId(),
                equipment.equipmentTypeName(),
                authorization);

        shiftRepository.save(shift);

        if (authorization != null) {
            LOGGER.warn(
                    "Asignación forzada en el turno {} {}: {} en {} autorizada por el usuario {} omitiendo {} — motivo: {}",
                    shift.getDate(),
                    shift.getJourney(),
                    operator.fullName(),
                    equipment.code(),
                    authorization.getAuthorizedByUserId(),
                    authorization.getOverriddenRuleCodeList(),
                    authorization.getReason());
        } else {
            LOGGER.info(
                    "Se asignó a {} en {} para el turno {} {}",
                    operator.fullName(),
                    equipment.code(),
                    shift.getDate(),
                    shift.getJourney());
        }

        return Optional.of(new AssignmentOutcome(assignment, evaluation.warnings()));
    }

    @Override
    @Transactional
    public Optional<Shift> handle(CancelAssignmentCommand command) {
        var shift = findShift(command.shiftId());
        shift.cancelAssignment(command.assignmentId(), command.reason());
        LOGGER.info("Se canceló la asignación {} del turno {}: {}",
                command.assignmentId(), shift.getId(), command.reason());
        return Optional.of(shiftRepository.save(shift));
    }

    /**
     * Decide si la asignación puede proceder, y construye la constancia de auditoría cuando procede
     * solo porque un supervisor lo autorizó.
     *
     * @return la autorización a adjuntar, o null cuando no hubo que levantar ninguna regla
     */
    private SupervisorAuthorization resolveAuthorization(
            AssignOperatorToShiftCommand command, AssignmentEvaluation evaluation, Shift shift) {
        if (evaluation.isAccepted()) {
            return null;
        }

        if (!command.forced()) {
            throw new BusinessRuleViolationException(
                    "La asignación incumple %d regla(s) de negocio".formatted(evaluation.blockingViolations().size()),
                    evaluation.violations());
        }

        // Algunas reglas describen una dotación que no podría existir físicamente. Ninguna firma las
        // vuelve verdaderas.
        if (!evaluation.canBeOverridden()) {
            throw new BusinessRuleViolationException(
                    "La asignación incumple reglas que nadie puede autorizar", evaluation.violations());
        }

        var supervisorId = userContextFacade.fetchUserIdByEmail(command.requestedByEmail())
                .orElseThrow(() -> new AccessDeniedException(
                        "No se pudo identificar al usuario que autoriza esta asignación"));
        if (!userContextFacade.isAllowedToAuthorizeOverrides(supervisorId)) {
            throw new AccessDeniedException(
                    "Solo un supervisor puede autorizar una asignación que incumple las reglas");
        }

        var supervisorName = userContextFacade.fetchFullNameByUserId(supervisorId).orElse("Desconocido");
        LOGGER.info("El turno {} recibió una asignación forzada autorizada por {}", shift.getId(), supervisorName);
        return new SupervisorAuthorization(
                supervisorId, supervisorName, command.authorizationReason(), evaluation.blockingViolations());
    }

    private Shift findShift(Long shiftId) {
        return shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("El turno", shiftId));
    }
}
