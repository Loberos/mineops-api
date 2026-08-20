package com.mineops.mineopsapi.assets.domain.model.queries;

/**
 * Lee el historial de mantenimiento, del más reciente al más antiguo.
 *
 * @param equipmentId restringe a una máquina; null devuelve el historial de toda la flota
 */
public record GetMaintenanceHistoryQuery(Long equipmentId) {

    public static GetMaintenanceHistoryQuery forFleet() {
        return new GetMaintenanceHistoryQuery(null);
    }
}
