package com.mineops.mineopsapi.operations.domain.services;

import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.operations.domain.model.queries.GetAllShiftsQuery;
import com.mineops.mineopsapi.operations.domain.model.queries.GetShiftByIdQuery;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;

import java.util.List;
import java.util.Optional;

public interface ShiftQueryService {

    /** Resuelve la consulta acotada al tramo pedido. Es la que atiende al listado de la API. */
    PagedResult<Shift> handle(GetAllShiftsQuery query, PageCriteria criteria);

    /** Resuelve la consulta completa, sin trocear, para los usos que no la exponen por HTTP. */
    List<Shift> handle(GetAllShiftsQuery query);

    Optional<Shift> handle(GetShiftByIdQuery query);
}
