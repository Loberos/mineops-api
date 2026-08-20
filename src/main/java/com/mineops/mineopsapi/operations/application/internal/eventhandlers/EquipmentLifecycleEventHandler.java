package com.mineops.mineopsapi.operations.application.internal.eventhandlers;

import com.mineops.mineopsapi.assets.domain.model.events.EquipmentBlockedEvent;
import com.mineops.mineopsapi.assets.domain.model.events.EquipmentReleasedEvent;
import com.mineops.mineopsapi.operations.infrastructure.persistence.jpa.repositories.ShiftRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Mantiene honesta la dotación cuando una máquina cambia de estado por debajo de ella.
 * <p>
 * <strong>La decisión que esto codifica.</strong> Una máquina se bloquea un miércoles y ya estaba
 * programada para el jueves, el viernes y el sábado. Borrar esas asignaciones tiraría el trabajo de un
 * planificador y, peor, perdería la constancia de que existieron. Dejarlas intactas permitiría que una
 * programación prometa en silencio máquinas que no pueden operar. Así que ni se borran ni se ignoran:
 * cada una se marca como en riesgo, con el motivo adjunto, y aparece en una lista de trabajo para que
 * una persona lo resuelva —reasignar al operador a otra máquina, o suspender la asignación. El sistema
 * levanta la alarma; la persona decide. Cuando la máquina se atiende y vuelve a trabajar, las marcas
 * que ella causó se levantan solas, porque a esa altura ya no hay nada que decidir.
 * </p>
 * <p>
 * El handler corre dentro de la transacción que cambió la máquina, así que el bloqueo y las marcas
 * aterrizan juntos o no aterrizan.
 * </p>
 */
@Component
public class EquipmentLifecycleEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EquipmentLifecycleEventHandler.class);

    private final ShiftRepository shiftRepository;

    public EquipmentLifecycleEventHandler(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onEquipmentBlocked(EquipmentBlockedEvent event) {
        var reason = "El equipo %s se bloqueó el %s con %s horas, siendo su umbral %s horas"
                .formatted(
                        event.equipmentCode(),
                        event.occurredAt().toLocalDate(),
                        event.hourMeter(),
                        event.thresholdHours());

        var shifts = shiftRepository.findPlannedShiftsWithEquipment(event.equipmentId(), LocalDate.now());
        var flagged = shifts.stream()
                .mapToInt(shift -> shift.flagAssignmentsForEquipment(event.equipmentId(), reason))
                .sum();

        if (flagged > 0) {
            shiftRepository.saveAll(shifts);
            LOGGER.warn(
                    "El equipo {} se bloqueó: {} asignación(es) programada(s) en {} turno(s) quedaron en riesgo",
                    event.equipmentCode(),
                    flagged,
                    shifts.size());
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onEquipmentReleased(EquipmentReleasedEvent event) {
        var shifts = shiftRepository.findPlannedShiftsWithEquipment(event.equipmentId(), LocalDate.now());
        if (shifts.isEmpty()) {
            return;
        }
        shifts.forEach(shift -> shift.clearRiskForEquipment(event.equipmentId()));
        shiftRepository.saveAll(shifts);
        LOGGER.info(
                "El equipo {} volvió a servicio: se restauraron las asignaciones que había dejado en riesgo",
                event.equipmentCode());
    }
}
