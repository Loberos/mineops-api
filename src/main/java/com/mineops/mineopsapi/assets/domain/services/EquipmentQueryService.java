package com.mineops.mineopsapi.assets.domain.services;

import com.mineops.mineopsapi.assets.domain.model.aggregates.Equipment;
import com.mineops.mineopsapi.assets.domain.model.queries.GetAllEquipmentQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetEquipmentByCodeQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetEquipmentByIdQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetFleetSummaryQuery;
import com.mineops.mineopsapi.assets.domain.model.valueobjects.FleetSummary;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;

import java.util.List;
import java.util.Optional;

public interface EquipmentQueryService {

    /**
     * Resuelve la consulta acotada al tramo pedido. Es la que atiende al listado de la API.
     */
    PagedResult<Equipment> handle(GetAllEquipmentQuery query, PageCriteria criteria);

    /**
     * Resuelve la consulta completa, sin trocear.
     * <p>
     * Queda para los usos internos que necesitan recorrer la flota entera y no la exponen por HTTP,
     * como el facade que consulta este contexto desde operaciones.
     * </p>
     */
    List<Equipment> handle(GetAllEquipmentQuery query);

    Optional<Equipment> handle(GetEquipmentByIdQuery query);

    Optional<Equipment> handle(GetEquipmentByCodeQuery query);

    /** Contadores medidos sobre la flota entera, para la cabecera del listado paginado. */
    FleetSummary handle(GetFleetSummaryQuery query);
}
