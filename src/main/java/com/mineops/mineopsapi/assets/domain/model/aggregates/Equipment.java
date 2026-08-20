package com.mineops.mineopsapi.assets.domain.model.aggregates;

import com.mineops.mineopsapi.assets.domain.model.events.EquipmentBlockedEvent;
import com.mineops.mineopsapi.assets.domain.model.events.EquipmentReleasedEvent;
import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;
import com.mineops.mineopsapi.assets.domain.model.valueobjects.MaintenanceCycle;
import com.mineops.mineopsapi.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Máquina de la flota, y agregado dueño de las reglas del horómetro.
 * <p>
 * Toda transición del ciclo de vida de mantenimiento ocurre aquí y no en un servicio: se acumula el
 * uso, se contrasta contra el umbral, y cruzarlo bloquea la máquina y anuncia el hecho. Nadie puede
 * subir el horómetro sin que el bloqueo se evalúe, y eso es lo que hace imposible saltarse la regla 2.
 * </p>
 */
@Entity
@Table(name = "equipment")
@Getter
public class Equipment extends AuditableAbstractAggregateRoot<Equipment> {

    @NotBlank
    @Size(max = 40)
    @Column(name = "code", length = 40, nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "equipment_type_id", nullable = false)
    private EquipmentType equipmentType;

    /** Horas de uso acumuladas. Solo avanza. */
    @Column(name = "hour_meter", nullable = false, precision = 12, scale = 2)
    private BigDecimal hourMeter;

    /** Lectura de horómetro a la que esta máquina debe detenerse para mantenimiento. */
    @Column(name = "maintenance_threshold_hours", nullable = false, precision = 12, scale = 2)
    private BigDecimal maintenanceThresholdHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private EquipmentStatus status;

    @Column(name = "last_maintenance_hour_meter", precision = 12, scale = 2)
    private BigDecimal lastMaintenanceHourMeter;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    /**
     * Bloqueo optimista. Sin él, dos supervisores cerrando turnos de la misma máquina al mismo tiempo
     * leerían el mismo horómetro y uno de los dos incrementos se perdería.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Equipment() {
        // Requerido por JPA.
    }

    public Equipment(String code, EquipmentType equipmentType, BigDecimal initialHourMeter) {
        this.code = normalizeCode(code);
        this.equipmentType = equipmentType;
        this.hourMeter = requireNonNegative(initialHourMeter, "El horómetro inicial");
        this.maintenanceThresholdHours = MaintenanceCycle
                .startingAt(equipmentType.getMaintenanceIntervalHours(), this.hourMeter)
                .thresholdHours();
        this.status = EquipmentStatus.AVAILABLE;
        evaluateThreshold();
    }

    /**
     * Ciclo que esta máquina está corriendo actualmente.
     */
    public MaintenanceCycle currentCycle() {
        return new MaintenanceCycle(equipmentType.getMaintenanceIntervalHours(), maintenanceThresholdHours);
    }

    /**
     * Suma las horas trabajadas durante un turno y bloquea la máquina si eso cruza el umbral.
     *
     * @param hours horas a sumar, no puede ser negativo
     * @return {@code true} cuando es esta llamada la que bloqueó la máquina
     */
    public boolean registerUsage(BigDecimal hours) {
        requireNonNegative(hours, "Las horas a registrar");
        var wasBlocked = status == EquipmentStatus.BLOCKED;
        this.hourMeter = this.hourMeter.add(hours);
        evaluateThreshold();
        return !wasBlocked && status == EquipmentStatus.BLOCKED;
    }

    /**
     * Registra un mantenimiento realizado: libera la máquina y abre su siguiente ciclo.
     *
     * @param readingAtMaintenance horómetro registrado por el taller; nunca menor al actual
     * @param performedOn          fecha en que se ejecutó el mantenimiento
     * @return el ciclo que se cerró, que lleva el umbral que estaba vencido y permite a quien llama
     *         registrar cuánto se pasó la máquina de él
     */
    public MaintenanceCycle completeMaintenance(BigDecimal readingAtMaintenance, LocalDate performedOn) {
        if (readingAtMaintenance.compareTo(hourMeter) < 0) {
            throw new IllegalArgumentException(
                    "El horómetro no puede retroceder: la máquina ya marca %s horas".formatted(hourMeter));
        }
        var closedCycle = currentCycle();
        this.hourMeter = readingAtMaintenance;
        this.lastMaintenanceHourMeter = readingAtMaintenance;
        this.lastMaintenanceDate = performedOn;
        this.maintenanceThresholdHours = closedCycle.nextAfterMaintenanceAt(readingAtMaintenance).thresholdHours();
        this.status = EquipmentStatus.AVAILABLE;
        registerEvent(EquipmentReleasedEvent.of(getId(), code, hourMeter, maintenanceThresholdHours));
        return closedCycle;
    }

    /**
     * Envía la máquina al taller antes de alcanzar su umbral, o reconoce que una máquina bloqueada ya
     * entró a él.
     */
    public void sendToWorkshop() {
        if (status == EquipmentStatus.OUT_OF_SERVICE) {
            throw new IllegalStateException("El equipo %s está fuera de servicio".formatted(code));
        }
        this.status = EquipmentStatus.IN_MAINTENANCE;
    }

    public void withdrawFromService() {
        this.status = EquipmentStatus.OUT_OF_SERVICE;
    }

    /**
     * Devuelve a la flota una máquina retirada de servicio, volviendo a contrastar su umbral.
     */
    public void returnToService() {
        if (status != EquipmentStatus.OUT_OF_SERVICE) {
            throw new IllegalStateException("El equipo %s no está fuera de servicio".formatted(code));
        }
        this.status = EquipmentStatus.AVAILABLE;
        evaluateThreshold();
    }

    public boolean isAvailableForAssignment() {
        return status.allowsAssignment();
    }

    /**
     * Horas de uso que quedan antes de que venza el mantenimiento. Negativo cuando ya se excedió.
     */
    public BigDecimal hoursUntilMaintenance() {
        return currentCycle().remainingAt(hourMeter);
    }

    /**
     * Cuánto marcaría el horómetro después de trabajar las horas indicadas. Lo usa la proyección para
     * mirar hacia adelante sin mutar nada.
     */
    public BigDecimal projectedHourMeterAfter(BigDecimal additionalHours) {
        return hourMeter.add(additionalHours);
    }

    /**
     * Bloquea la máquina cuando el horómetro alcanzó el umbral, y lo anuncia para que puedan
     * revisarse los turnos que ya estaban programados con ella.
     */
    private void evaluateThreshold() {
        if (status != EquipmentStatus.AVAILABLE) {
            return;
        }
        if (currentCycle().isReachedAt(hourMeter)) {
            this.status = EquipmentStatus.BLOCKED;
            registerEvent(EquipmentBlockedEvent.of(getId(), code, hourMeter, maintenanceThresholdHours));
        }
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código del equipo es obligatorio");
        }
        return code.trim().toUpperCase();
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String subject) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("%s no puede ser negativo".formatted(subject));
        }
        return value;
    }
}
