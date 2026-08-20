package com.mineops.mineopsapi.workforce.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Ventana durante la cual una certificación está vigente, con ambos extremos incluidos.
 * <p>
 * Mantener las dos fechas juntas como un valor es lo que permite formular en una sola llamada la
 * pregunta que realmente importa: <em>¿esta certificación cubre el turno completo?</em>, en lugar de
 * que cada llamador la vuelva a deducir.
 * </p>
 */
@Embeddable
@Getter
@EqualsAndHashCode
public class ValidityPeriod {

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    protected ValidityPeriod() {
        // Requerido por JPA.
    }

    public ValidityPeriod(LocalDate issuedOn, LocalDate expiresOn) {
        if (issuedOn == null || expiresOn == null) {
            throw new IllegalArgumentException(
                    "Una certificación requiere fecha de emisión y fecha de vencimiento");
        }
        if (expiresOn.isBefore(issuedOn)) {
            throw new IllegalArgumentException("Una certificación no puede vencer antes de haberse emitido");
        }
        this.issuedOn = issuedOn;
        this.expiresOn = expiresOn;
    }

    /**
     * Indica si la certificación está vigente en el día indicado.
     */
    public boolean isValidOn(LocalDate date) {
        return !date.isBefore(issuedOn) && !date.isAfter(expiresOn);
    }

    /**
     * Indica si la certificación cubre todos los días del rango indicado.
     * <p>
     * Esta es la comprobación que necesita un turno: un turno de noche que empieza el día en que la
     * certificación vence termina al día siguiente, y medio turno certificado no es un turno
     * certificado.
     * </p>
     */
    public boolean coversRange(LocalDate from, LocalDate to) {
        return isValidOn(from) && isValidOn(to);
    }

    public boolean isExpiredOn(LocalDate date) {
        return date.isAfter(expiresOn);
    }

    /**
     * Días que faltan para que la certificación venza, contados desde el día indicado. Negativo una
     * vez vencida.
     */
    public long daysUntilExpiryFrom(LocalDate date) {
        return ChronoUnit.DAYS.between(date, expiresOn);
    }
}
