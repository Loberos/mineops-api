package com.mineops.mineopsapi.assets.domain.model.queries;

/**
 * Pide los contadores de cabecera de la flota. No lleva parámetros: siempre mide la flota entera,
 * que es justamente lo que el listado paginado ya no puede contar por su cuenta.
 */
public record GetFleetSummaryQuery() {
}
