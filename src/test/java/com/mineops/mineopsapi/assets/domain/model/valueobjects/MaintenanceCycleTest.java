package com.mineops.mineopsapi.assets.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("Ciclo de mantenimiento")
class MaintenanceCycleTest {

    private static final BigDecimal INTERVAL = BigDecimal.valueOf(250);

    private static MaintenanceCycle cycleDueAt(int threshold) {
        return new MaintenanceCycle(INTERVAL, BigDecimal.valueOf(threshold));
    }

    private static BigDecimal hours(int value) {
        return BigDecimal.valueOf(value);
    }

    @Nested
    @DisplayName("al calcular el siguiente umbral")
    class NextThreshold {

        @Test
        @DisplayName("conserva el ritmo planificado cuando el mantenimiento fue tardío, sin arrastrar el atraso")
        void absorbsTheDelayWhenServicedLate() {
            var next = cycleDueAt(250).nextAfterMaintenanceAt(hours(280));

            // Contar desde la lectura real habría dado 530 y habría dejado cada parada futura más
            // fuera de compás.
            assertThat(next.thresholdHours()).isEqualByComparingTo(hours(500));
        }

        @Test
        @DisplayName("no acumula el atraso a lo largo de varios mantenimientos tardíos")
        void doesNotAccumulateDelayAcrossCycles() {
            var first = cycleDueAt(250).nextAfterMaintenanceAt(hours(280));
            var second = first.nextAfterMaintenanceAt(hours(530));
            var third = second.nextAfterMaintenanceAt(hours(775));

            assertThat(first.thresholdHours()).isEqualByComparingTo(hours(500));
            assertThat(second.thresholdHours()).isEqualByComparingTo(hours(750));
            assertThat(third.thresholdHours()).isEqualByComparingTo(hours(1000));
        }

        @Test
        @DisplayName("cuenta desde la lectura real cuando el mantenimiento se adelantó")
        void restartsFromTheReadingWhenServicedEarly() {
            var next = cycleDueAt(250).nextAfterMaintenanceAt(hours(200));

            // Atender una máquina antes de tiempo no debe castigarse con un ciclo más corto.
            assertThat(next.thresholdHours()).isEqualByComparingTo(hours(450));
        }

        @Test
        @DisplayName("mantiene el umbral por delante de la lectura cuando el atraso superó un ciclo completo")
        void catchesUpWhenTheDelayExceedsAnEntireCycle() {
            var next = cycleDueAt(250).nextAfterMaintenanceAt(hours(620));

            // 500 y 750 ya quedaron atrás; el primer umbral que sigue por delante es 750.
            assertThat(next.thresholdHours()).isEqualByComparingTo(hours(750));
        }

        @Test
        @DisplayName("avanza al ciclo siguiente cuando el mantenimiento ocurrió justo en el umbral")
        void movesOnWhenServicedExactlyOnTime() {
            var next = cycleDueAt(250).nextAfterMaintenanceAt(hours(250));

            assertThat(next.thresholdHours()).isEqualByComparingTo(hours(500));
        }
    }

    @Nested
    @DisplayName("al medir el desfase")
    class Overrun {

        @Test
        @DisplayName("informa cuánto se pasó la máquina de su umbral")
        void reportsTheHoursBeyondTheThreshold() {
            assertThat(cycleDueAt(250).overrunAt(hours(280))).isEqualByComparingTo(hours(30));
        }

        @Test
        @DisplayName("nunca informa un desfase negativo para una máquina atendida antes de tiempo")
        void reportsZeroWhenServicedEarly() {
            assertThat(cycleDueAt(250).overrunAt(hours(200))).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("al consultar el margen restante")
    class Remaining {

        @Test
        @DisplayName("informa las horas que faltan para el umbral")
        void reportsHoursLeft() {
            assertThat(cycleDueAt(250).remainingAt(hours(242))).isEqualByComparingTo(hours(8));
        }

        @Test
        @DisplayName("se vuelve negativo una vez que la máquina se excedió")
        void goesNegativeWhenOverdue() {
            assertThat(cycleDueAt(250).remainingAt(hours(263))).isEqualByComparingTo(hours(-13));
        }

        @Test
        @DisplayName("considera alcanzado el umbral exactamente en el umbral")
        void reachesTheThresholdOnEquality() {
            assertThat(cycleDueAt(250).isReachedAt(hours(250))).isTrue();
            assertThat(cycleDueAt(250).isReachedAt(hours(249))).isFalse();
        }
    }

    @Test
    @DisplayName("rechaza un intervalo que no sea positivo")
    void rejectsANonPositiveInterval() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MaintenanceCycle(BigDecimal.ZERO, hours(250)));
    }

    @Test
    @DisplayName("abre el primer ciclo un intervalo por delante de la lectura actual")
    void opensTheFirstCycleFromTheCurrentReading() {
        var cycle = MaintenanceCycle.startingAt(INTERVAL, hours(1000));

        assertThat(cycle.thresholdHours()).isEqualByComparingTo(hours(1250));
    }
}
