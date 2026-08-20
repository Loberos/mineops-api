package com.mineops.mineopsapi.workforce.domain.model.queries;

/**
 * Certificaciones que vencen dentro del horizonte indicado, de la más próxima a la más lejana.
 * <p>
 * Las ya vencidas se incluyen a propósito: son las que impiden a un operador trabajar hoy.
 * </p>
 *
 * @param withinDays días hacia adelante a considerar, contados desde hoy
 */
public record GetExpiringCertificationsQuery(int withinDays) {

    public GetExpiringCertificationsQuery {
        if (withinDays < 0) {
            throw new IllegalArgumentException("El horizonte no puede ser negativo");
        }
    }
}
