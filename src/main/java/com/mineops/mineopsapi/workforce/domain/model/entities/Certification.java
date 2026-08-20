package com.mineops.mineopsapi.workforce.domain.model.entities;

import com.mineops.mineopsapi.workforce.domain.model.aggregates.Operator;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.ValidityPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Habilitación que permite a un operador conducir una familia de máquinas durante un periodo.
 * <p>
 * La familia se referencia por identificador y no por objeto, porque los tipos de equipo pertenecen
 * al contexto de activos. El código se guarda al lado como copia, de modo que leer una certificación
 * nunca obligue a entrar en otro contexto.
 * </p>
 */
@Entity
@Table(name = "certifications")
@Getter
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @Column(name = "equipment_type_id", nullable = false)
    private Long equipmentTypeId;

    @NotBlank
    @Size(max = 40)
    @Column(name = "equipment_type_code", length = 40, nullable = false)
    private String equipmentTypeCode;

    @NotBlank
    @Size(max = 120)
    @Column(name = "equipment_type_name", length = 120, nullable = false)
    private String equipmentTypeName;

    @Embedded
    private ValidityPeriod validity;

    protected Certification() {
        // Requerido por JPA.
    }

    /**
     * Pensado para invocarse solo desde {@link Operator#certifyFor}, que es lo que mantiene
     * verificable la regla "como máximo una certificación por familia de máquinas".
     */
    public Certification(
            Operator operator,
            Long equipmentTypeId,
            String equipmentTypeCode,
            String equipmentTypeName,
            ValidityPeriod validity) {
        this.operator = operator;
        this.equipmentTypeId = equipmentTypeId;
        this.equipmentTypeCode = equipmentTypeCode;
        this.equipmentTypeName = equipmentTypeName;
        this.validity = validity;
    }

    /**
     * Extiende o corrige la ventana de una certificación existente, que es en lo que consiste una
     * renovación.
     */
    public void renew(ValidityPeriod validity) {
        this.validity = validity;
    }

    public boolean isForEquipmentType(Long equipmentTypeId) {
        return this.equipmentTypeId.equals(equipmentTypeId);
    }

    public boolean isValidOn(LocalDate date) {
        return validity.isValidOn(date);
    }

    public boolean coversRange(LocalDate from, LocalDate to) {
        return validity.coversRange(from, to);
    }
}
