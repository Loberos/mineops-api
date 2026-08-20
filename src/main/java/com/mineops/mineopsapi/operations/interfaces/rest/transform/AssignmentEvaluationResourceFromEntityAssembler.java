package com.mineops.mineopsapi.operations.interfaces.rest.transform;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentEvaluation;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.AssignmentEvaluationResource;
import com.mineops.mineopsapi.shared.interfaces.rest.resources.RuleViolationResource;

public final class AssignmentEvaluationResourceFromEntityAssembler {

    private AssignmentEvaluationResourceFromEntityAssembler() {
    }

    public static AssignmentEvaluationResource toResourceFromEntity(AssignmentEvaluation evaluation) {
        return new AssignmentEvaluationResource(
                evaluation.isAccepted(),
                evaluation.canBeOverridden(),
                evaluation.blockingViolations().stream().map(RuleViolationResource::fromViolation).toList(),
                evaluation.warnings().stream().map(RuleViolationResource::fromViolation).toList());
    }
}
