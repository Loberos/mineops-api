package com.mineops.mineopsapi.operations.domain.services.rules;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;

import java.util.Optional;

/**
 * Una regla de negocio que una asignación debe satisfacer.
 * <p>
 * Una regla informa lo que encuentra en lugar de lanzar una excepción, y esa es toda la razón por la
 * que el sistema puede responder "esto es todo lo que está mal con esta asignación" en vez de "esto
 * es lo primero que salió mal". Sumar una regla al sistema significa agregar una clase que implemente
 * esta interfaz; nada más tiene que cambiar.
 * </p>
 */
public interface AssignmentRule {

    /**
     * @param context la asignación propuesta y todo lo necesario para juzgarla
     * @return la violación, o vacío cuando esta regla se cumple
     */
    Optional<BusinessRuleViolation> evaluate(AssignmentContext context);
}
