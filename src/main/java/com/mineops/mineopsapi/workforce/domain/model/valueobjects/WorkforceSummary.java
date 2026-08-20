package com.mineops.mineopsapi.workforce.domain.model.valueobjects;

/**
 * Los contadores de cabecera de la dotación, medidos sobre la dotación entera.
 * <p>
 * Igual que su equivalente en la flota, existe porque el listado viaja paginado y un conteo sobre
 * la página en curso no describiría a la dotación.
 * </p>
 *
 * @param total                     operadores registrados
 * @param withoutValidCertification operadores que hoy no pueden programarse porque ninguna de sus
 *                                  certificaciones está vigente
 */
public record WorkforceSummary(long total, long withoutValidCertification) {
}
