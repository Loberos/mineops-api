package com.mineops.mineopsapi.workforce.domain.model.aggregates;

import com.mineops.mineopsapi.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.mineops.mineopsapi.workforce.domain.model.entities.Certification;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.OperatorStatus;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.PersonName;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.ValidityPeriod;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Persona que conduce las máquinas, y agregado dueño de sus certificaciones.
 * <p>
 * Las certificaciones viven dentro de esta frontera porque no tienen sentido separadas del operador
 * que las posee: otorgar, renovar o revocar una es un cambio al operador, y el invariante "como
 * máximo una certificación por familia de máquinas" solo puede hacerse cumplir desde aquí.
 * </p>
 */
@Entity
@Table(name = "operators")
@Getter
public class Operator extends AuditableAbstractAggregateRoot<Operator> {

    @NotBlank
    @Size(max = 20)
    @Column(name = "document_number", length = 20, nullable = false, unique = true)
    private String documentNumber;

    @Embedded
    private PersonName name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OperatorStatus status;

    @OneToMany(
            mappedBy = "operator",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<Certification> certifications = new ArrayList<>();

    protected Operator() {
        // Requerido por JPA.
    }

    public Operator(String documentNumber, PersonName name) {
        this.documentNumber = normalizeDocument(documentNumber);
        this.name = name;
        this.status = OperatorStatus.ACTIVE;
        this.certifications = new ArrayList<>();
    }

    public void rename(PersonName name) {
        this.name = name;
    }

    public void activate() {
        this.status = OperatorStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = OperatorStatus.INACTIVE;
    }

    public boolean isAvailableForAssignment() {
        return status.allowsAssignment();
    }

    /**
     * Certifica al operador para una familia de máquinas, o renueva la certificación que ya tiene.
     * <p>
     * Una segunda certificación para la misma familia es una renovación, nunca un duplicado: mantener
     * dos filas vivas volvería ambigua la pregunta "¿está certificado?" justo cuando más importa.
     * </p>
     *
     * @param equipmentTypeId   familia para la que se certifica al operador
     * @param equipmentTypeCode código de la familia, que se guarda como copia
     * @param equipmentTypeName nombre de la familia, que se guarda como copia
     * @param validity          ventana durante la cual rige la certificación
     * @return la certificación, ya sea recién otorgada o renovada
     */
    public Certification certifyFor(
            Long equipmentTypeId, String equipmentTypeCode, String equipmentTypeName, ValidityPeriod validity) {
        var existing = certificationFor(equipmentTypeId);
        if (existing.isPresent()) {
            existing.get().renew(validity);
            return existing.get();
        }
        var certification =
                new Certification(this, equipmentTypeId, equipmentTypeCode, equipmentTypeName, validity);
        this.certifications.add(certification);
        return certification;
    }

    /**
     * Elimina una certificación por completo, para el caso en que se haya otorgado por error.
     *
     * @return si se eliminó algo
     */
    public boolean revokeCertificationFor(Long equipmentTypeId) {
        return this.certifications.removeIf(certification -> certification.isForEquipmentType(equipmentTypeId));
    }

    public Optional<Certification> certificationFor(Long equipmentTypeId) {
        return certifications.stream()
                .filter(certification -> certification.isForEquipmentType(equipmentTypeId))
                .findFirst();
    }

    /**
     * Indica si este operador puede conducir la familia de máquinas indicada en el día indicado.
     */
    public boolean isCertifiedFor(Long equipmentTypeId, LocalDate date) {
        return certificationFor(equipmentTypeId)
                .map(certification -> certification.isValidOn(date))
                .orElse(false);
    }

    /**
     * Indica si la certificación para la familia indicada cubre todo un periodo, ambos extremos
     * incluidos.
     * <p>
     * Es lo que pregunta un turno: una certificación que caduca a mitad de camino no alcanza.
     * </p>
     */
    public boolean isCertifiedThroughout(Long equipmentTypeId, LocalDate from, LocalDate to) {
        return certificationFor(equipmentTypeId)
                .map(certification -> certification.coversRange(from, to))
                .orElse(false);
    }

    /**
     * Certificaciones que vencen dentro del horizonte indicado, de la más próxima a la más lejana.
     * Las ya vencidas se incluyen, porque son lo más urgente de la lista.
     */
    public List<Certification> certificationsExpiringBefore(LocalDate limit) {
        return certifications.stream()
                .filter(certification -> !certification.getValidity().getExpiresOn().isAfter(limit))
                .sorted(Comparator.comparing(certification -> certification.getValidity().getExpiresOn()))
                .toList();
    }

    private static String normalizeDocument(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("El número de documento es obligatorio");
        }
        return documentNumber.trim().toUpperCase();
    }
}
