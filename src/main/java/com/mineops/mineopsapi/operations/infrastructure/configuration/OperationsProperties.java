package com.mineops.mineopsapi.operations.infrastructure.configuration;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Umbrales operativos externalizados, enlazados desde el prefijo {@code mineops.operations}.
 *
 * @param shiftClosureTolerance cuánto puede apartarse un cierre del plan, como fracción de las horas
 *                              planificadas, antes de exigir justificación por escrito
 * @param projectionHorizonDays días hacia adelante que mira la proyección de mantenimiento
 */
@Validated
@ConfigurationProperties(prefix = "mineops.operations")
public record OperationsProperties(
        @DecimalMin(value = "0.0") BigDecimal shiftClosureTolerance,
        @Min(1) int projectionHorizonDays) {
}
