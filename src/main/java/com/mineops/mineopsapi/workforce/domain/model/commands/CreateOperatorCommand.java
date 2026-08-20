package com.mineops.mineopsapi.workforce.domain.model.commands;

/**
 * Agrega un operador a la plantilla.
 *
 * @param documentNumber documento de identidad, único
 * @param firstName      nombres
 * @param lastName       apellidos
 */
public record CreateOperatorCommand(String documentNumber, String firstName, String lastName) {
}
