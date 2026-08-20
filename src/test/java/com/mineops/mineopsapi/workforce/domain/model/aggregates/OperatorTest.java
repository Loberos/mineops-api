package com.mineops.mineopsapi.workforce.domain.model.aggregates;

import com.mineops.mineopsapi.workforce.domain.model.valueobjects.OperatorStatus;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.PersonName;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.ValidityPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("Operador")
class OperatorTest {

    private static final Long HAUL_TRUCK = 1L;
    private static final Long EXCAVATOR = 2L;
    private static final LocalDate TODAY = LocalDate.now();

    private static Operator anOperator() {
        return new Operator("45678901", new PersonName("Juan", "Quispe"));
    }

    private static ValidityPeriod window(LocalDate from, LocalDate to) {
        return new ValidityPeriod(from, to);
    }

    @Test
    @DisplayName("entra a la plantilla activo y sin certificaciones")
    void joinsActiveAndUncertified() {
        var operator = anOperator();

        assertThat(operator.getStatus()).isEqualTo(OperatorStatus.ACTIVE);
        assertThat(operator.getCertifications()).isEmpty();
        assertThat(operator.isCertifiedFor(HAUL_TRUCK, TODAY)).isFalse();
    }

    @Test
    @DisplayName("se certifica para una familia de máquinas por un periodo de tiempo")
    void isCertifiedForAPeriod() {
        var operator = anOperator();

        operator.certifyFor(HAUL_TRUCK, "HAUL_TRUCK", "Camión de acarreo",
                window(TODAY.minusYears(1), TODAY.plusMonths(6)));

        assertThat(operator.isCertifiedFor(HAUL_TRUCK, TODAY)).isTrue();
        assertThat(operator.isCertifiedFor(EXCAVATOR, TODAY)).isFalse();
    }

    @Test
    @DisplayName("renueva una certificación existente en vez de tener dos para la misma familia")
    void renewsInsteadOfDuplicating() {
        var operator = anOperator();
        operator.certifyFor(HAUL_TRUCK, "HAUL_TRUCK", "Camión de acarreo",
                window(TODAY.minusYears(2), TODAY.minusDays(1)));

        operator.certifyFor(HAUL_TRUCK, "HAUL_TRUCK", "Camión de acarreo",
                window(TODAY, TODAY.plusYears(1)));

        assertThat(operator.getCertifications()).hasSize(1);
        assertThat(operator.isCertifiedFor(HAUL_TRUCK, TODAY)).isTrue();
    }

    @Test
    @DisplayName("no está certificado en un día fuera de la ventana")
    void isNotCertifiedOutsideTheWindow() {
        var operator = anOperator();
        operator.certifyFor(HAUL_TRUCK, "HAUL_TRUCK", "Camión de acarreo",
                window(TODAY.minusYears(2), TODAY.minusDays(10)));

        assertThat(operator.isCertifiedFor(HAUL_TRUCK, TODAY)).isFalse();
    }

    @Test
    @DisplayName("exige que la certificación cubra todo el periodo, no solo su primer día")
    void requiresTheWholePeriodToBeCovered() {
        var operator = anOperator();
        var lastValidDay = TODAY.plusDays(3);
        operator.certifyFor(HAUL_TRUCK, "HAUL_TRUCK", "Camión de acarreo",
                window(TODAY.minusYears(1), lastValidDay));

        assertThat(operator.isCertifiedThroughout(HAUL_TRUCK, lastValidDay, lastValidDay)).isTrue();
        assertThat(operator.isCertifiedThroughout(HAUL_TRUCK, lastValidDay, lastValidDay.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("lista las certificaciones próximas a vencer, de la más cercana a la más lejana")
    void listsTheCertificationsAboutToLapse() {
        var operator = anOperator();
        operator.certifyFor(HAUL_TRUCK, "HAUL_TRUCK", "Camión de acarreo",
                window(TODAY.minusYears(1), TODAY.plusDays(20)));
        operator.certifyFor(EXCAVATOR, "EXCAVATOR", "Excavadora",
                window(TODAY.minusYears(1), TODAY.plusDays(5)));

        var expiring = operator.certificationsExpiringBefore(TODAY.plusDays(30));

        assertThat(expiring).hasSize(2);
        assertThat(expiring.getFirst().getEquipmentTypeId()).isEqualTo(EXCAVATOR);
    }

    @Test
    @DisplayName("puede revocársele una certificación por completo")
    void revokesACertification() {
        var operator = anOperator();
        operator.certifyFor(HAUL_TRUCK, "HAUL_TRUCK", "Camión de acarreo",
                window(TODAY.minusYears(1), TODAY.plusYears(1)));

        var revoked = operator.revokeCertificationFor(HAUL_TRUCK);

        assertThat(revoked).isTrue();
        assertThat(operator.getCertifications()).isEmpty();
    }

    @Test
    @DisplayName("no puede programarse una vez que dejó la plantilla")
    void cannotBeScheduledWhenInactive() {
        var operator = anOperator();

        operator.deactivate();

        assertThat(operator.isAvailableForAssignment()).isFalse();
    }

    @Test
    @DisplayName("rechaza una certificación que vence antes de haberse emitido")
    void refusesAnInvertedWindow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> window(TODAY, TODAY.minusDays(1)))
                .withMessageContaining("vencer antes");
    }
}
