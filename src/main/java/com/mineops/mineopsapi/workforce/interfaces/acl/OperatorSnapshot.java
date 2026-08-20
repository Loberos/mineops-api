package com.mineops.mineopsapi.workforce.interfaces.acl;

import com.mineops.mineopsapi.workforce.domain.model.aggregates.Operator;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.OperatorStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Vista de solo lectura de un operador junto con las certificaciones que posee.
 * <p>
 * El contexto de operaciones evalúa sus reglas de asignación contra esta copia, y por eso las
 * preguntas sobre certificaciones se responden aquí en lugar de reimplementarse en cada regla.
 * </p>
 *
 * @param id             identificador del operador
 * @param documentNumber documento de identidad
 * @param fullName       nombre visible
 * @param status         si el operador forma parte actualmente de la plantilla
 * @param certifications todas las certificaciones que posee, vigentes o no
 */
public record OperatorSnapshot(
        Long id,
        String documentNumber,
        String fullName,
        OperatorStatus status,
        List<CertificationSnapshot> certifications) {

    public static OperatorSnapshot fromAggregate(Operator operator) {
        return new OperatorSnapshot(
                operator.getId(),
                operator.getDocumentNumber(),
                operator.getName().getFullName(),
                operator.getStatus(),
                operator.getCertifications().stream().map(CertificationSnapshot::fromEntity).toList());
    }

    public boolean isAvailableForAssignment() {
        return status.allowsAssignment();
    }

    public Optional<CertificationSnapshot> certificationFor(Long equipmentTypeId) {
        return certifications.stream()
                .filter(certification -> certification.equipmentTypeId().equals(equipmentTypeId))
                .findFirst();
    }

    /**
     * Indica si el operador puede conducir la familia de máquinas indicada durante todo un periodo.
     */
    public boolean isCertifiedThroughout(Long equipmentTypeId, LocalDate from, LocalDate to) {
        return certificationFor(equipmentTypeId)
                .map(certification -> certification.coversRange(from, to))
                .orElse(false);
    }
}
