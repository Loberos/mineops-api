package com.mineops.mineopsapi.assets.domain.services;

import com.mineops.mineopsapi.assets.domain.model.aggregates.MaintenanceRecord;
import com.mineops.mineopsapi.assets.domain.model.queries.GetMaintenanceHistoryQuery;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;

import java.util.List;

public interface MaintenanceQueryService {

    /**
     * Resuelve el historial acotado al tramo pedido. Es la que atiende a los listados de la API.
     */
    PagedResult<MaintenanceRecord> handle(GetMaintenanceHistoryQuery query, PageCriteria criteria);

    /**
     * Resuelve el historial completo, sin trocear. Queda para los usos que necesitan recorrerlo
     * entero y no lo exponen por HTTP.
     */
    List<MaintenanceRecord> handle(GetMaintenanceHistoryQuery query);
}
