package com.mineops.mineopsapi.operations.domain.model.aggregates;

import com.mineops.mineopsapi.operations.domain.model.entities.Assignment;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentStatus;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.Journey;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.ShiftStatus;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.SupervisorAuthorization;
import com.mineops.mineopsapi.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Un turno de trabajo y la dotación asociada a él.
 * <p>
 * <strong>Por qué las asignaciones viven dentro de este agregado.</strong> Las dos reglas que dicen
 * que un operador no puede tener dos asignaciones en el mismo turno, y que un equipo no puede estar
 * asignado dos veces en el mismo turno, son afirmaciones sobre el turno como un todo. Hacer del turno
 * la frontera de consistencia significa que esas reglas se verifican contra un único objeto cargado
 * en una única transacción, y que la versión de ese objeto es lo que hace que dos supervisores
 * editando la misma dotación choquen en lugar de pisarse en silencio.
 * </p>
 */
@Entity
@Table(
        name = "shifts",
        uniqueConstraints = @UniqueConstraint(name = "uk_shifts_date_journey", columnNames = {"date", "journey"}))
@Getter
public class Shift extends AuditableAbstractAggregateRoot<Shift> {

    private static final int MINUTES_PER_HOUR = 60;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "journey", length = 10, nullable = false)
    private Journey journey;

    /** Horas que se espera que dure el turno. */
    @Column(name = "planned_hours", nullable = false, precision = 12, scale = 2)
    private BigDecimal plannedHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ShiftStatus status;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Assignment> assignments = new ArrayList<>();

    /**
     * Bloqueo optimista sobre toda la dotación. Dos supervisores que cargan el mismo turno y cada uno
     * agrega una asignación escribirán ambos contra esta versión; el segundo pierde y se le pide
     * reintentar contra datos frescos en lugar de sobrescribir al primero.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Shift() {
        // Requerido por JPA.
    }

    public Shift(LocalDate date, Journey journey, BigDecimal plannedHours, String notes) {
        if (date == null) {
            throw new IllegalArgumentException("La fecha del turno es obligatoria");
        }
        if (journey == null) {
            throw new IllegalArgumentException("La jornada del turno es obligatoria");
        }
        if (plannedHours == null || plannedHours.signum() <= 0) {
            throw new IllegalArgumentException("Las horas planificadas deben ser mayores que cero");
        }
        this.date = date;
        this.journey = journey;
        this.plannedHours = plannedHours;
        this.notes = notes;
        this.status = ShiftStatus.PLANNED;
        this.assignments = new ArrayList<>();
    }

    public LocalDateTime startsAt() {
        return LocalDateTime.of(date, journey.startsAt());
    }

    /**
     * Cuándo termina el turno, derivado de su inicio y su duración planificada.
     */
    public LocalDateTime endsAt() {
        var minutes = plannedHours
                .multiply(BigDecimal.valueOf(MINUTES_PER_HOUR))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return startsAt().plusMinutes(minutes);
    }

    /**
     * Día calendario en que termina el turno, que es el siguiente para un turno de noche largo.
     */
    public LocalDate endDate() {
        return endsAt().toLocalDate();
    }

    public boolean isOpen() {
        return status.acceptsAssignments();
    }

    public boolean isInFutureFrom(LocalDate reference) {
        return date.isAfter(reference);
    }

    /**
     * Las asignaciones que todavía retienen a su operador y a su máquina.
     */
    public List<Assignment> activeAssignments() {
        return assignments.stream().filter(Assignment::occupiesResources).toList();
    }

    public List<Assignment> openAssignments() {
        return assignments.stream()
                .filter(assignment -> assignment.getStatus().isOpen())
                .toList();
    }

    public boolean hasOperatorAssigned(Long operatorId) {
        return activeAssignments().stream().anyMatch(assignment -> assignment.isForOperator(operatorId));
    }

    public boolean hasEquipmentAssigned(Long equipmentId) {
        return activeAssignments().stream().anyMatch(assignment -> assignment.isForEquipment(equipmentId));
    }

    public Optional<Assignment> findAssignment(Long assignmentId) {
        return assignments.stream()
                .filter(assignment -> Objects.equals(assignment.getId(), assignmentId))
                .findFirst();
    }

    /**
     * Agrega una asignación a la dotación.
     * <p>
     * Las reglas que cruzan contextos —si la máquina está disponible, si el operador está
     * certificado— las evalúa antes el motor de reglas, que necesita leer otros contextos. Lo que el
     * agregado garantiza aquí es la parte que solo él puede ver: que el turno siga abierto y que ni el
     * operador ni la máquina estén ya ocupados.
     * </p>
     *
     * @param authorization la autorización del supervisor cuando la asignación incumple una regla, o null
     * @return la asignación que se agregó
     */
    public Assignment assign(
            Long operatorId,
            String operatorName,
            String operatorDocument,
            Long equipmentId,
            String equipmentCode,
            Long equipmentTypeId,
            String equipmentTypeName,
            SupervisorAuthorization authorization) {
        if (!isOpen()) {
            throw new IllegalStateException("El turno %s %s no está abierto".formatted(date, journey));
        }
        if (hasOperatorAssigned(operatorId)) {
            throw new IllegalStateException("El operador ya está asignado a este turno");
        }
        if (hasEquipmentAssigned(equipmentId)) {
            throw new IllegalStateException("El equipo ya está asignado a este turno");
        }
        var assignment = new Assignment(
                this,
                operatorId,
                operatorName,
                operatorDocument,
                equipmentId,
                equipmentCode,
                equipmentTypeId,
                equipmentTypeName,
                authorization);
        this.assignments.add(assignment);
        return assignment;
    }

    /**
     * Marca todas las asignaciones abiertas de la máquina indicada, porque ya no puede trabajar.
     *
     * @return cuántas asignaciones se marcaron
     */
    public int flagAssignmentsForEquipment(Long equipmentId, String reason) {
        return (int) assignments.stream()
                .filter(assignment -> assignment.isForEquipment(equipmentId))
                .filter(assignment -> assignment.flagAtRisk(reason))
                .count();
    }

    /**
     * Levanta la marca de riesgo de las asignaciones de una máquina que puede volver a trabajar.
     */
    public void clearRiskForEquipment(Long equipmentId) {
        assignments.stream()
                .filter(assignment -> assignment.isForEquipment(equipmentId))
                .forEach(Assignment::clearRisk);
    }

    public void cancelAssignment(Long assignmentId, String reason) {
        if (!isOpen()) {
            throw new IllegalStateException("Un turno cerrado ya no puede editarse");
        }
        var assignment = findAssignment(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La asignación %s no pertenece a este turno".formatted(assignmentId)));
        assignment.cancel(reason);
    }

    /**
     * Liquida el turno. Quien llama debe haber completado antes todas sus asignaciones, que es lo que
     * registra las horas trabajadas contra cada máquina.
     */
    public void close() {
        if (!isOpen()) {
            throw new IllegalStateException("El turno %s %s ya está liquidado".formatted(date, journey));
        }
        var pending = assignments.stream().anyMatch(assignment -> assignment.getStatus().isOpen());
        if (pending) {
            throw new IllegalStateException("Toda asignación debe liquidarse antes de cerrar el turno");
        }
        this.status = ShiftStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    /**
     * Suspende un turno que no se trabajó, liberando todas sus asignaciones.
     */
    public void cancel(String reason) {
        if (status == ShiftStatus.CLOSED) {
            throw new IllegalStateException("Un turno cerrado no puede cancelarse");
        }
        this.assignments.stream()
                .filter(assignment -> assignment.getStatus() != AssignmentStatus.CANCELLED)
                .forEach(assignment -> assignment.cancel(reason));
        this.status = ShiftStatus.CANCELLED;
        this.notes = reason;
    }

    public void updatePlan(BigDecimal plannedHours, String notes) {
        if (!isOpen()) {
            throw new IllegalStateException("Un turno liquidado ya no puede replanificarse");
        }
        if (plannedHours == null || plannedHours.signum() <= 0) {
            throw new IllegalArgumentException("Las horas planificadas deben ser mayores que cero");
        }
        this.plannedHours = plannedHours;
        this.notes = notes;
    }
}
