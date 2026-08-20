package com.mineops.mineopsapi.assets.interfaces.rest.resources;

import com.mineops.mineopsapi.assets.domain.model.valueobjects.FleetSummary;

/**
 * Contadores de cabecera de la flota.
 *
 * @param total         máquinas en la flota
 * @param blocked       máquinas bloqueadas
 * @param nearThreshold máquinas disponibles con menos del diez por ciento de su ciclo restante
 */
public record FleetSummaryResource(long total, long blocked, long nearThreshold) {

    public static FleetSummaryResource fromSummary(FleetSummary summary) {
        return new FleetSummaryResource(summary.total(), summary.blocked(), summary.nearThreshold());
    }
}
