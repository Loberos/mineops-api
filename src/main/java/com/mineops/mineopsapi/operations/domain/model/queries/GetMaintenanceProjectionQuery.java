package com.mineops.mineopsapi.operations.domain.model.queries;

/**
 * Regla de negocio 12: qué máquinas alcanzarán su umbral de mantenimiento dentro del horizonte, dados
 * los turnos que ya están en la programación.
 *
 * @param horizonDays días hacia adelante a considerar, contados desde hoy
 */
public record GetMaintenanceProjectionQuery(int horizonDays) {

    public GetMaintenanceProjectionQuery {
        if (horizonDays < 1) {
            throw new IllegalArgumentException("El horizonte de proyección debe cubrir al menos un día");
        }
    }
}
