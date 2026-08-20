package com.mineops.mineopsapi.operations.domain.model.entities;

import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentStatus;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.SupervisorAuthorization;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Un operador conduciendo una máquina durante un turno.
 * <p>
 * El operador y la máquina pertenecen a otros contextos, así que se referencian por identificador y
 * se describen con una copia de su nombre en ese momento. Eso mantiene legible la dotación años
 * después, cuando un operador ya dejó la empresa y una máquina se renumeró.
 * </p>
 */
@Entity
@Table(name = "assignments")
@Getter
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "operator_name", length = 160, nullable = false)
    private String operatorName;

    @Column(name = "operator_document", length = 20, nullable = false)
    private String operatorDocument;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "equipment_code", length = 40, nullable = false)
    private String equipmentCode;

    @Column(name = "equipment_type_id", nullable = false)
    private Long equipmentTypeId;

    @Column(name = "equipment_type_name", length = 120, nullable = false)
    private String equipmentTypeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AssignmentStatus status;

    /** Horas efectivamente trabajadas, que solo se conocen al cerrar el turno. */
    @Column(name = "worked_hours", precision = 12, scale = 2)
    private BigDecimal workedHours;

    /** Explica un cierre que se apartó del plan, o por qué se canceló la asignación. */
    @Column(name = "closure_note", length = 500)
    private String closureNote;

    /** Por qué la asignación está en riesgo. Nulo mientras nada la amenace. */
    @Column(name = "risk_reason", length = 500)
    private String riskReason;

    @Embedded
    private SupervisorAuthorization authorization;

    protected Assignment() {
        // Requerido por JPA.
    }

    public Assignment(
            Shift shift,
            Long operatorId,
            String operatorName,
            String operatorDocument,
            Long equipmentId,
            String equipmentCode,
            Long equipmentTypeId,
            String equipmentTypeName,
            SupervisorAuthorization authorization) {
        this.shift = shift;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
        this.operatorDocument = operatorDocument;
        this.equipmentId = equipmentId;
        this.equipmentCode = equipmentCode;
        this.equipmentTypeId = equipmentTypeId;
        this.equipmentTypeName = equipmentTypeName;
        this.authorization = authorization;
        this.status = AssignmentStatus.SCHEDULED;
    }

    public boolean isForced() {
        return authorization != null;
    }

    public boolean isForOperator(Long operatorId) {
        return this.operatorId.equals(operatorId);
    }

    public boolean isForEquipment(Long equipmentId) {
        return this.equipmentId.equals(equipmentId);
    }

    /**
     * Indica si esta asignación todavía retiene a su operador y a su máquina para el turno.
     */
    public boolean occupiesResources() {
        return status.occupiesResources();
    }

    /**
     * Marca la asignación porque algo cambió después de haberla planificado. Es idempotente, así que
     * una máquina bloqueada dos veces no sobrescribe la primera explicación con otra idéntica.
     *
     * @param reason qué cambió
     * @return si es esta llamada la que puso la asignación en riesgo
     */
    public boolean flagAtRisk(String reason) {
        if (status != AssignmentStatus.SCHEDULED) {
            return false;
        }
        this.status = AssignmentStatus.AT_RISK;
        this.riskReason = reason;
        return true;
    }

    /**
     * Levanta la marca cuando el obstáculo desaparece, por ejemplo después de atender la máquina.
     */
    public void clearRisk() {
        if (status == AssignmentStatus.AT_RISK) {
            this.status = AssignmentStatus.SCHEDULED;
            this.riskReason = null;
        }
    }

    public void cancel(String reason) {
        if (status == AssignmentStatus.COMPLETED) {
            throw new IllegalStateException("Una asignación completada no puede cancelarse");
        }
        this.status = AssignmentStatus.CANCELLED;
        this.closureNote = reason;
    }

    /**
     * Liquida la asignación con las horas efectivamente trabajadas.
     *
     * @param workedHours horas efectivamente trabajadas durante el turno
     * @param note        justificación, obligatoria cuando el cierre se aparta del plan
     */
    public void complete(BigDecimal workedHours, String note) {
        if (!status.isOpen()) {
            throw new IllegalStateException(
                    "La asignación %s no está abierta y no puede cerrarse".formatted(id));
        }
        this.workedHours = workedHours;
        this.closureNote = note;
        this.status = AssignmentStatus.COMPLETED;
    }

    /**
     * Diferencia entre las horas trabajadas y las planificadas para el turno. Positivo cuando el turno
     * se extendió. Cero mientras la asignación no se haya cerrado.
     */
    public BigDecimal hoursVariance() {
        if (workedHours == null) {
            return BigDecimal.ZERO;
        }
        return workedHours.subtract(shift.getPlannedHours());
    }
}
