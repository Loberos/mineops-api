package com.mineops.mineopsapi.assets.domain.model.aggregates;

import com.mineops.mineopsapi.assets.domain.model.valueobjects.MaintenanceCycle;
import com.mineops.mineopsapi.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una entrada del historial de mantenimiento de una máquina.
 * <p>
 * El registro es inmutable una vez escrito y guarda una copia del código del equipo en lugar de un
 * join: una traza de auditoría debe describir lo que era cierto cuando ocurrió el hecho, y debe
 * sobrevivir a que la máquina se renumere. Guarda además el umbral que estaba vencido y el desfase,
 * que es lo que hace visible el atraso acumulado en vez de esconderlo dentro del siguiente umbral.
 * </p>
 */
@Entity
@Table(name = "maintenance_records")
@Getter
public class MaintenanceRecord extends AuditableAbstractAggregateRoot<MaintenanceRecord> {

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @NotBlank
    @Size(max = 40)
    @Column(name = "equipment_code", length = 40, nullable = false)
    private String equipmentCode;

    @Column(name = "performed_on", nullable = false)
    private LocalDate performedOn;

    /** Horómetro registrado por el taller al atender la máquina. */
    @Column(name = "hour_meter", nullable = false, precision = 12, scale = 2)
    private BigDecimal hourMeter;

    /** Umbral que estaba vencido, para que el historial muestre lo que debió haber pasado. */
    @Column(name = "threshold_hours", nullable = false, precision = 12, scale = 2)
    private BigDecimal thresholdHours;

    /** Horas que la máquina operó más allá del umbral antes de ser atendida. Cero si fue a tiempo. */
    @Column(name = "overrun_hours", nullable = false, precision = 12, scale = 2)
    private BigDecimal overrunHours;

    /** Umbral que abrió este mantenimiento. */
    @Column(name = "next_threshold_hours", nullable = false, precision = 12, scale = 2)
    private BigDecimal nextThresholdHours;

    @NotBlank
    @Size(max = 120)
    @Column(name = "responsible", length = 120, nullable = false)
    private String responsible;

    @Size(max = 1000)
    @Column(name = "observations", length = 1000)
    private String observations;

    protected MaintenanceRecord() {
        // Requerido por JPA.
    }

    private MaintenanceRecord(
            Equipment equipment,
            MaintenanceCycle closedCycle,
            LocalDate performedOn,
            BigDecimal hourMeter,
            String responsible,
            String observations) {
        this.equipmentId = equipment.getId();
        this.equipmentCode = equipment.getCode();
        this.performedOn = performedOn;
        this.hourMeter = hourMeter;
        this.thresholdHours = closedCycle.thresholdHours();
        this.overrunHours = closedCycle.overrunAt(hourMeter);
        this.nextThresholdHours = equipment.getMaintenanceThresholdHours();
        this.responsible = responsible.trim();
        this.observations = observations;
    }

    /**
     * Construye la entrada de historial de un mantenimiento que ya se aplicó a la máquina.
     *
     * @param equipment    la máquina, ya liberada por {@code completeMaintenance}
     * @param closedCycle  el ciclo que cerró el mantenimiento
     * @param performedOn  la fecha en que se ejecutó el trabajo
     * @param hourMeter    la lectura tomada por el taller
     * @param responsible  quién ejecutó o dio conformidad al trabajo
     * @param observations notas libres, opcionales
     * @return la entrada de historial, lista para almacenarse
     */
    public static MaintenanceRecord forCompletedMaintenance(
            Equipment equipment,
            MaintenanceCycle closedCycle,
            LocalDate performedOn,
            BigDecimal hourMeter,
            String responsible,
            String observations) {
        return new MaintenanceRecord(equipment, closedCycle, performedOn, hourMeter, responsible, observations);
    }

    /**
     * Indica si la máquina fue atendida después de lo que correspondía.
     */
    public boolean wasOverdue() {
        return overrunHours.signum() > 0;
    }
}
