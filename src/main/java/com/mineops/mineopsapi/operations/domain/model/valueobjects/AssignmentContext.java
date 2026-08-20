package com.mineops.mineopsapi.operations.domain.model.valueobjects;

import com.mineops.mineopsapi.assets.interfaces.acl.EquipmentSnapshot;
import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.workforce.interfaces.acl.OperatorSnapshot;

/**
 * Todo lo que las reglas necesitan para juzgar una asignación propuesta, reunido de una sola vez.
 * <p>
 * Armar el contexto por adelantado es lo que permite que cada regla sea una función pura: ninguna
 * regla consulta nada, así que todas pueden evaluarse y recogerse sus veredictos, en lugar de que el
 * primer incumplimiento corte la verificación.
 * </p>
 *
 * @param shift     el turno al que se sumaría la asignación, con su dotación actual
 * @param operator  el operador propuesto, con las certificaciones que posee
 * @param equipment la máquina propuesta, con su horómetro y su estado
 */
public record AssignmentContext(Shift shift, OperatorSnapshot operator, EquipmentSnapshot equipment) {

    public AssignmentContext {
        if (shift == null || operator == null || equipment == null) {
            throw new IllegalArgumentException("Una asignación debe nombrar un turno, un operador y una máquina");
        }
    }
}
