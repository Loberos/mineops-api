package com.mineops.mineopsapi.operations.domain.services;

import com.mineops.mineopsapi.operations.domain.model.queries.GetMaintenanceProjectionQuery;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.ProjectedMaintenance;

import java.util.List;

/**
 * Regla de negocio 12: mira hacia adelante en la programación en lugar de mirar el estado presente.
 */
public interface MaintenanceProjectionQueryService {

    List<ProjectedMaintenance> handle(GetMaintenanceProjectionQuery query);
}
