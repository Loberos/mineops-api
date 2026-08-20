package com.mineops.mineopsapi.workforce.interfaces.rest.resources;

import com.mineops.mineopsapi.workforce.domain.model.valueobjects.WorkforceSummary;

/**
 * Contadores de cabecera de la dotación.
 *
 * @param total                     operadores registrados
 * @param withoutValidCertification operadores sin ninguna certificación vigente hoy
 */
public record WorkforceSummaryResource(long total, long withoutValidCertification) {

    public static WorkforceSummaryResource fromSummary(WorkforceSummary summary) {
        return new WorkforceSummaryResource(summary.total(), summary.withoutValidCertification());
    }
}
