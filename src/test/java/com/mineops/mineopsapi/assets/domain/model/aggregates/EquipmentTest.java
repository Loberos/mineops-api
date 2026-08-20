package com.mineops.mineopsapi.assets.domain.model.aggregates;

import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("Equipo")
class EquipmentTest {

    private static final BigDecimal INTERVAL = BigDecimal.valueOf(250);

    private static Equipment haulTruckAt(int hoursOfUse) {
        var type = new EquipmentType("HAUL_TRUCK", "Camión de acarreo", INTERVAL, null);
        var equipment = new Equipment("CAM-001", type, BigDecimal.ZERO);
        if (hoursOfUse > 0) {
            equipment.registerUsage(BigDecimal.valueOf(hoursOfUse));
        }
        return equipment;
    }

    private static BigDecimal hours(int value) {
        return BigDecimal.valueOf(value);
    }

    @Test
    @DisplayName("entra a la flota disponible, con su primer umbral un intervalo por delante")
    void startsAvailableWithItsFirstThreshold() {
        var equipment = haulTruckAt(0);

        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        assertThat(equipment.getMaintenanceThresholdHours()).isEqualByComparingTo(hours(250));
        assertThat(equipment.isAvailableForAssignment()).isTrue();
    }

    @Test
    @DisplayName("sigue disponible mientras las horas trabajadas lo mantengan por debajo del umbral")
    void staysAvailableBelowTheThreshold() {
        var equipment = haulTruckAt(200);

        var blocked = equipment.registerUsage(hours(42));

        assertThat(blocked).isFalse();
        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        assertThat(equipment.hoursUntilMaintenance()).isEqualByComparingTo(hours(8));
    }

    @Test
    @DisplayName("se bloquea solo en el momento en que las horas trabajadas alcanzan el umbral")
    void blocksItselfOnReachingTheThreshold() {
        var equipment = haulTruckAt(242);

        var blocked = equipment.registerUsage(hours(12));

        assertThat(blocked).isTrue();
        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.BLOCKED);
        assertThat(equipment.isAvailableForAssignment()).isFalse();
        assertThat(equipment.getHourMeter()).isEqualByComparingTo(hours(254));
    }

    @Test
    @DisplayName("informa el bloqueo una sola vez, por más horas que se sumen después")
    void reportsTheBlockOnlyOnce() {
        var equipment = haulTruckAt(242);
        equipment.registerUsage(hours(12));

        var blockedAgain = equipment.registerUsage(hours(5));

        assertThat(blockedAgain).isFalse();
        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.BLOCKED);
    }

    @Test
    @DisplayName("lo libera su mantenimiento, que además abre el siguiente ciclo")
    void isReleasedByItsMaintenance() {
        var equipment = haulTruckAt(280);
        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.BLOCKED);

        var closedCycle = equipment.completeMaintenance(hours(280), LocalDate.now());

        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        assertThat(closedCycle.thresholdHours()).isEqualByComparingTo(hours(250));
        assertThat(closedCycle.overrunAt(hours(280))).isEqualByComparingTo(hours(30));
        assertThat(equipment.getMaintenanceThresholdHours()).isEqualByComparingTo(hours(500));
        assertThat(equipment.getLastMaintenanceHourMeter()).isEqualByComparingTo(hours(280));
    }

    @Test
    @DisplayName("acepta del taller una lectura mayor a la que conocía")
    void acceptsAHigherReadingFromTheWorkshop() {
        var equipment = haulTruckAt(250);

        equipment.completeMaintenance(hours(268), LocalDate.now());

        assertThat(equipment.getHourMeter()).isEqualByComparingTo(hours(268));
    }

    @Test
    @DisplayName("rechaza una lectura que haría retroceder su horómetro")
    void refusesAReadingThatGoesBackwards() {
        var equipment = haulTruckAt(250);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> equipment.completeMaintenance(hours(240), LocalDate.now()))
                .withMessageContaining("retroceder");
    }

    @Test
    @DisplayName("rechaza que se le sumen horas negativas")
    void refusesNegativeUsage() {
        var equipment = haulTruckAt(100);

        assertThatIllegalArgumentException().isThrownBy(() -> equipment.registerUsage(hours(-5)));
    }

    @Test
    @DisplayName("no puede programarse mientras está en el taller")
    void cannotBeScheduledWhileInTheWorkshop() {
        var equipment = haulTruckAt(100);

        equipment.sendToWorkshop();

        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.IN_MAINTENANCE);
        assertThat(equipment.isAvailableForAssignment()).isFalse();
    }

    @Test
    @DisplayName("vuelve a contrastar su umbral al regresar de estar retirado")
    void reChecksItsThresholdOnReturningToService() {
        var equipment = haulTruckAt(240);
        equipment.withdrawFromService();
        // Horas acumuladas mientras estaba fuera de registro, por ejemplo una lectura corregida por el taller.
        equipment.registerUsage(hours(20));

        equipment.returnToService();

        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.BLOCKED);
    }

    @Test
    @DisplayName("proyecta una lectura futura sin cambiar nada")
    void projectsWithoutMutating() {
        var equipment = haulTruckAt(200);

        var projected = equipment.projectedHourMeterAfter(hours(12));

        assertThat(projected).isEqualByComparingTo(hours(212));
        assertThat(equipment.getHourMeter()).isEqualByComparingTo(hours(200));
    }
}
