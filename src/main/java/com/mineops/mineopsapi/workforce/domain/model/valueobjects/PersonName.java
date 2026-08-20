package com.mineops.mineopsapi.workforce.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Nombre de una persona, modelado como valor para que sus dos mitades viajen siempre juntas y se
 * formateen igual en todas partes.
 */
@Embeddable
@Getter
@EqualsAndHashCode
public class PersonName {

    @NotBlank
    @Size(max = 80)
    @Column(name = "first_name", length = 80, nullable = false)
    private String firstName;

    @NotBlank
    @Size(max = 80)
    @Column(name = "last_name", length = 80, nullable = false)
    private String lastName;

    protected PersonName() {
        // Requerido por JPA.
    }

    public PersonName(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("El apellido es obligatorio");
        }
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }

    public String getFullName() {
        return "%s %s".formatted(firstName, lastName);
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
