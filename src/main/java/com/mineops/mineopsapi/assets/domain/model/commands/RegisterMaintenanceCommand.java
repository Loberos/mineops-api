package com.mineops.mineopsapi.assets.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registra un mantenimiento ya ejecutado, liberando la máquina y abriendo su siguiente ciclo.
 *
 * @param equipmentId  la máquina atendida
 * @param performedOn  fecha en que se ejecutó el trabajo
 * @param hourMeter    lectura tomada por el taller; si viene nula se usa la lectura actual
 * @param responsible  quién ejecutó o dio conformidad al trabajo
 * @param observations notas libres, opcionales
 */
public record RegisterMaintenanceCommand(
        Long equipmentId, LocalDate performedOn, BigDecimal hourMeter, String responsible, String observations) {
}
