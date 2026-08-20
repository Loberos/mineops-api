package com.mineops.mineopsapi.operations;

import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;
import com.mineops.mineopsapi.assets.interfaces.acl.EquipmentSnapshot;
import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.Journey;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.OperatorStatus;
import com.mineops.mineopsapi.workforce.interfaces.acl.CertificationSnapshot;
import com.mineops.mineopsapi.workforce.interfaces.acl.OperatorSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Constructores de los objetos que necesitan las pruebas de operaciones, para que cada prueba se lea
 * como el escenario que describe y no como una pila de argumentos de constructor.
 */
public final class OperationsFixtures {

    public static final Long HAUL_TRUCK_TYPE_ID = 1L;
    public static final Long EXCAVATOR_TYPE_ID = 2L;

    private OperationsFixtures() {
    }

    public static Shift dayShift(LocalDate date, int plannedHours) {
        return new Shift(date, Journey.DAY, BigDecimal.valueOf(plannedHours), null);
    }

    public static Shift nightShift(LocalDate date, int plannedHours) {
        return new Shift(date, Journey.NIGHT, BigDecimal.valueOf(plannedHours), null);
    }

    public static OperatorSnapshot certifiedOperator(Long id, CertificationSnapshot... certifications) {
        return new OperatorSnapshot(
                id, "4567890" + id, "Operador " + id, OperatorStatus.ACTIVE, List.of(certifications));
    }

    public static OperatorSnapshot inactiveOperator(Long id, CertificationSnapshot... certifications) {
        return new OperatorSnapshot(
                id, "4567890" + id, "Operador " + id, OperatorStatus.INACTIVE, List.of(certifications));
    }

    public static CertificationSnapshot certification(Long equipmentTypeId, LocalDate issuedOn, LocalDate expiresOn) {
        return new CertificationSnapshot(equipmentTypeId, "TIPO-" + equipmentTypeId, "Tipo " + equipmentTypeId,
                issuedOn, expiresOn);
    }

    public static CertificationSnapshot validCertification(Long equipmentTypeId) {
        var today = LocalDate.now();
        return certification(equipmentTypeId, today.minusYears(1), today.plusYears(1));
    }

    public static CertificationSnapshot expiredCertification(Long equipmentTypeId) {
        var today = LocalDate.now();
        return certification(equipmentTypeId, today.minusYears(2), today.minusDays(10));
    }

    public static EquipmentSnapshot availableEquipment(Long id, String code, int hourMeter, int threshold) {
        return equipment(id, code, EquipmentStatus.AVAILABLE, hourMeter, threshold);
    }

    public static EquipmentSnapshot blockedEquipment(Long id, String code) {
        return equipment(id, code, EquipmentStatus.BLOCKED, 260, 250);
    }

    public static EquipmentSnapshot equipment(
            Long id, String code, EquipmentStatus status, int hourMeter, int threshold) {
        return new EquipmentSnapshot(
                id,
                code,
                HAUL_TRUCK_TYPE_ID,
                "HAUL_TRUCK",
                "Camión de acarreo",
                status,
                BigDecimal.valueOf(hourMeter),
                BigDecimal.valueOf(threshold),
                BigDecimal.valueOf(250));
    }

    /**
     * Asigna un operador y una máquina a un turno, tal como ya los contendría la dotación.
     */
    public static void book(Shift shift, OperatorSnapshot operator, EquipmentSnapshot equipment) {
        shift.assign(
                operator.id(),
                operator.fullName(),
                operator.documentNumber(),
                equipment.id(),
                equipment.code(),
                equipment.equipmentTypeId(),
                equipment.equipmentTypeName(),
                null);
    }
}
