package com.mineops.mineopsapi.operations.application.internal.queryservices;

import com.mineops.mineopsapi.assets.interfaces.acl.EquipmentContextFacade;
import com.mineops.mineopsapi.operations.domain.model.entities.Assignment;
import com.mineops.mineopsapi.operations.domain.model.queries.GetAssignmentsAtRiskQuery;
import com.mineops.mineopsapi.operations.domain.model.queries.PreviewAssignmentQuery;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentEvaluation;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentStatus;
import com.mineops.mineopsapi.operations.domain.services.AssignmentQueryService;
import com.mineops.mineopsapi.operations.domain.services.AssignmentRuleEvaluator;
import com.mineops.mineopsapi.operations.infrastructure.persistence.jpa.repositories.ShiftRepository;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import com.mineops.mineopsapi.workforce.interfaces.acl.OperatorContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AssignmentQueryServiceImpl implements AssignmentQueryService {

    private final ShiftRepository shiftRepository;
    private final AssignmentRuleEvaluator ruleEvaluator;
    private final OperatorContextFacade operatorContextFacade;
    private final EquipmentContextFacade equipmentContextFacade;

    public AssignmentQueryServiceImpl(
            ShiftRepository shiftRepository,
            AssignmentRuleEvaluator ruleEvaluator,
            OperatorContextFacade operatorContextFacade,
            EquipmentContextFacade equipmentContextFacade) {
        this.shiftRepository = shiftRepository;
        this.ruleEvaluator = ruleEvaluator;
        this.operatorContextFacade = operatorContextFacade;
        this.equipmentContextFacade = equipmentContextFacade;
    }

    /**
     * Ejecuta el mismo motor de reglas que ejecuta el lado de escritura, contra los mismos datos, y no
     * escribe nada. Compartir el motor es lo que impide que la vista previa se desincronice de la
     * decisión real.
     */
    @Override
    public AssignmentEvaluation handle(PreviewAssignmentQuery query) {
        var shift = shiftRepository.findById(query.shiftId())
                .orElseThrow(() -> new ResourceNotFoundException("El turno", query.shiftId()));
        var operator = operatorContextFacade.fetchOperatorById(query.operatorId())
                .orElseThrow(() -> new ResourceNotFoundException("El operador", query.operatorId()));
        var equipment = equipmentContextFacade.fetchEquipmentById(query.equipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("El equipo", query.equipmentId()));
        return ruleEvaluator.evaluate(new AssignmentContext(shift, operator, equipment));
    }

    @Override
    public List<Assignment> handle(GetAssignmentsAtRiskQuery query) {
        return shiftRepository.findAssignmentsByStatus(AssignmentStatus.AT_RISK);
    }
}
