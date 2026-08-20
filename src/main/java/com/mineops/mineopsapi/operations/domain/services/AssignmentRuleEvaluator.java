package com.mineops.mineopsapi.operations.domain.services;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentEvaluation;
import com.mineops.mineopsapi.operations.domain.services.rules.AssignmentRule;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Contrasta una asignación propuesta contra todas las reglas y recoge sus veredictos.
 * <p>
 * Aquí es donde se satisface la regla de negocio 11, y se satisface por construcción y no por
 * acordarse: el evaluador no tiene forma de cortar antes, así que una petición que incumple cuatro
 * reglas vuelve con cuatro razones. Las reglas llegan por inyección en el orden en que se declararon,
 * de modo que una nueva entra en vigor con solo existir como bean; esta clase nunca tiene que
 * editarse para enterarse de ella.
 * </p>
 *
 * @implNote No hay interfaz sobre este servicio a propósito. No tiene infraestructura que
 *         reemplazar: es lógica de dominio pura sobre las reglas que recibe, y una segunda
 *         implementación solo podría ser una copia.
 */
@Service
public class AssignmentRuleEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentRuleEvaluator.class);

    private final List<AssignmentRule> rules;

    public AssignmentRuleEvaluator(List<AssignmentRule> rules) {
        this.rules = List.copyOf(rules);
        LOGGER.info("Motor de reglas de asignación cargado con {} reglas", this.rules.size());
    }

    /**
     * @param context la asignación propuesta
     * @return todas las reglas que incumple y todas las advertencias que levanta
     */
    public AssignmentEvaluation evaluate(AssignmentContext context) {
        List<BusinessRuleViolation> violations = rules.stream()
                .map(rule -> rule.evaluate(context))
                .flatMap(Optional::stream)
                .toList();
        return new AssignmentEvaluation(violations);
    }
}
