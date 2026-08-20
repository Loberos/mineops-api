package com.mineops.mineopsapi.assets.domain.model.aggregates;

import com.mineops.mineopsapi.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Familia de máquinas que comparten un régimen de mantenimiento: camiones de acarreo, excavadoras,
 * perforadoras.
 * <p>
 * El tipo es lo que lleva el intervalo de mantenimiento y aquello para lo que se certifica a un
 * operador, así que se modela como agregado de primera clase en vez de como una simple enumeración.
 * Agregar una nueva familia de máquinas pasa a ser una operación de datos y no un despliegue.
 * </p>
 */
@Entity
@Table(name = "equipment_types")
@Getter
public class EquipmentType extends AuditableAbstractAggregateRoot<EquipmentType> {

    @NotBlank
    @Size(max = 40)
    @Column(name = "code", length = 40, nullable = false, unique = true)
    private String code;

    @NotBlank
    @Size(max = 120)
    @Column(name = "name", length = 120, nullable = false)
    private String name;

    /** Horas de uso entre dos paradas de mantenimiento, por ejemplo 250. */
    @Positive
    @Column(name = "maintenance_interval_hours", nullable = false, precision = 12, scale = 2)
    private BigDecimal maintenanceIntervalHours;

    @Size(max = 400)
    @Column(name = "description", length = 400)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected EquipmentType() {
        // Requerido por JPA.
    }

    public EquipmentType(String code, String name, BigDecimal maintenanceIntervalHours, String description) {
        this.code = normalizeCode(code);
        this.name = name.trim();
        this.maintenanceIntervalHours = requirePositiveInterval(maintenanceIntervalHours);
        this.description = description;
        this.active = true;
    }

    public void update(String name, BigDecimal maintenanceIntervalHours, String description) {
        this.name = name.trim();
        this.maintenanceIntervalHours = requirePositiveInterval(maintenanceIntervalHours);
        this.description = description;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código del tipo de equipo es obligatorio");
        }
        return code.trim().toUpperCase();
    }

    private static BigDecimal requirePositiveInterval(BigDecimal interval) {
        if (interval == null || interval.signum() <= 0) {
            throw new IllegalArgumentException("El intervalo de mantenimiento debe ser mayor que cero");
        }
        return interval;
    }
}
