package com.mineops.mineopsapi.operations.domain.model.commands;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.Journey;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Programa un turno. La fecha y la jornada juntas lo identifican, de modo que hay a lo sumo un turno
 * de día y uno de noche por día calendario.
 *
 * @param date         día calendario al que pertenece el turno
 * @param journey      día o noche
 * @param plannedHours horas que se espera que dure el turno
 * @param notes        notas libres, opcionales
 */
public record CreateShiftCommand(LocalDate date, Journey journey, BigDecimal plannedHours, String notes) {
}
