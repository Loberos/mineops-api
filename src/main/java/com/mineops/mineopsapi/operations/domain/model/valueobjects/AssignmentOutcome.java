package com.mineops.mineopsapi.operations.domain.model.valueobjects;

import com.mineops.mineopsapi.operations.domain.model.entities.Assignment;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;

import java.util.List;

/**
 * Resultado de una asignación exitosa.
 * <p>
 * Lleva las advertencias junto con la asignación porque un comando aceptado igual puede tener algo
 * que decir: lo más frecuente es que este sea el turno que llevará a la máquina más allá de su umbral
 * de mantenimiento. Informar todo aplica a lo que pasa, no solo a lo que se rechaza.
 * </p>
 *
 * @param assignment la asignación que se creó
 * @param warnings   cosas que el planificador debería saber pero que no la impidieron
 */
public record AssignmentOutcome(Assignment assignment, List<BusinessRuleViolation> warnings) {

    public AssignmentOutcome {
        warnings = List.copyOf(warnings);
    }
}
