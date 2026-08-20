package com.mineops.mineopsapi.assets.domain.model.valueobjects;

/**
 * Los contadores de cabecera de la flota, medidos sobre la flota entera.
 * <p>
 * Existen porque el listado se entrega paginado: contar sobre la página que se acaba de recibir
 * daría un número que cambia al pasar de página, y traerse la flota completa solo para contarla
 * anularía el motivo de paginar. La base resuelve estos tres conteos sin mover filas hasta el
 * cliente.
 * </p>
 *
 * @param total         máquinas en la flota
 * @param blocked       máquinas bloqueadas por haber alcanzado su umbral
 * @param nearThreshold máquinas todavía disponibles a las que les queda menos del diez por ciento
 *                      de su ciclo
 */
public record FleetSummary(long total, long blocked, long nearThreshold) {
}
