package com.mineops.mineopsapi.workforce.domain.model.queries;

import com.mineops.mineopsapi.workforce.domain.model.valueobjects.OperatorStatus;

/**
 * @param status deja solo los operadores en este estado; null deja todos los estados
 */
public record GetAllOperatorsQuery(OperatorStatus status) {

    public static GetAllOperatorsQuery unfiltered() {
        return new GetAllOperatorsQuery(null);
    }
}
