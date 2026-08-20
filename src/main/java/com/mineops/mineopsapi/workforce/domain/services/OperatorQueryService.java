package com.mineops.mineopsapi.workforce.domain.services;

import com.mineops.mineopsapi.workforce.domain.model.aggregates.Operator;
import com.mineops.mineopsapi.workforce.domain.model.entities.Certification;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetAllOperatorsQuery;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetExpiringCertificationsQuery;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetOperatorByDocumentNumberQuery;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetOperatorByIdQuery;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetWorkforceSummaryQuery;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.WorkforceSummary;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;

import java.util.List;
import java.util.Optional;

/**
 * Lado de lectura del agregado de operador.
 */
public interface OperatorQueryService {

    /** Resuelve la consulta acotada al tramo pedido. Es la que atiende al listado de la API. */
    PagedResult<Operator> handle(GetAllOperatorsQuery query, PageCriteria criteria);

    /** Resuelve la consulta completa, sin trocear, para los usos que no la exponen por HTTP. */
    List<Operator> handle(GetAllOperatorsQuery query);

    Optional<Operator> handle(GetOperatorByIdQuery query);

    Optional<Operator> handle(GetOperatorByDocumentNumberQuery query);

    List<Certification> handle(GetExpiringCertificationsQuery query);

    /** Contadores medidos sobre la dotación entera, para la cabecera del listado paginado. */
    WorkforceSummary handle(GetWorkforceSummaryQuery query);
}
