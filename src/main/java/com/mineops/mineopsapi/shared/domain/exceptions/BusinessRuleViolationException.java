package com.mineops.mineopsapi.shared.domain.exceptions;

import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import lombok.Getter;

import java.util.List;

/**
 * Se lanza cuando un comando se rechaza por incumplir una o más reglas de negocio.
 * <p>
 * La excepción transporta <em>todas</em> las violaciones detectadas, nunca solo la primera, para que
 * a quien la reciba se le puedan informar todas las razones de una sola vez.
 * </p>
 */
@Getter
public class BusinessRuleViolationException extends RuntimeException {

    private final transient List<BusinessRuleViolation> violations;

    public BusinessRuleViolationException(String message, List<BusinessRuleViolation> violations) {
        super(message);
        this.violations = List.copyOf(violations);
    }
}
