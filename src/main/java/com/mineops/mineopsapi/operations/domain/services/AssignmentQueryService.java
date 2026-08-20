package com.mineops.mineopsapi.operations.domain.services;

import com.mineops.mineopsapi.operations.domain.model.entities.Assignment;
import com.mineops.mineopsapi.operations.domain.model.queries.GetAssignmentsAtRiskQuery;
import com.mineops.mineopsapi.operations.domain.model.queries.PreviewAssignmentQuery;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentEvaluation;

import java.util.List;

public interface AssignmentQueryService {

    /**
     * Evalúa una asignación propuesta sin crearla, devolviendo todas las reglas que incumpliría.
     */
    AssignmentEvaluation handle(PreviewAssignmentQuery query);

    List<Assignment> handle(GetAssignmentsAtRiskQuery query);
}
