package com.mineops.mineopsapi.operations.domain.model.valueobjects;

import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Constancia que queda cuando un supervisor autoriza una asignación que incumplía las reglas.
 * <p>
 * Una excepción que no deja rastro es indistinguible de un error de programación, así que la
 * autorización captura <em>quién</em> la permitió, <em>cuándo</em>, <em>por qué</em> y —la parte que
 * se olvida con facilidad— exactamente qué reglas se omitieron. Los códigos se congelan aquí en vez
 * de volver a deducirse después, porque para cuando alguien lea esto es muy posible que la máquina ya
 * haya sido atendida y la certificación renovada, y lo que se está preguntando es qué se sabía en el
 * momento de la decisión.
 * </p>
 */
@Embeddable
@Getter
@EqualsAndHashCode
public class SupervisorAuthorization {

    private static final String CODE_SEPARATOR = ",";

    @Column(name = "authorized_by_user_id")
    private Long authorizedByUserId;

    @Column(name = "authorized_by_name", length = 120)
    private String authorizedByName;

    @Column(name = "authorization_reason", length = 500)
    private String reason;

    @Column(name = "authorized_at")
    private LocalDateTime authorizedAt;

    /** Códigos de las reglas que se omitieron, unidos por comas. */
    @Column(name = "overridden_rule_codes", length = 500)
    private String overriddenRuleCodes;

    protected SupervisorAuthorization() {
        // Requerido por JPA.
    }

    public SupervisorAuthorization(
            Long authorizedByUserId,
            String authorizedByName,
            String reason,
            List<BusinessRuleViolation> overriddenViolations) {
        if (authorizedByUserId == null) {
            throw new IllegalArgumentException(
                    "Una asignación forzada requiere el supervisor que la autorizó");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Una asignación forzada requiere un motivo");
        }
        this.authorizedByUserId = authorizedByUserId;
        this.authorizedByName = authorizedByName;
        this.reason = reason.trim();
        this.authorizedAt = LocalDateTime.now();
        this.overriddenRuleCodes = overriddenViolations.stream()
                .map(BusinessRuleViolation::code)
                .distinct()
                .collect(Collectors.joining(CODE_SEPARATOR));
    }

    /**
     * Los códigos de las reglas omitidas, de vuelta como lista.
     */
    public List<String> getOverriddenRuleCodeList() {
        if (overriddenRuleCodes == null || overriddenRuleCodes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(overriddenRuleCodes.split(CODE_SEPARATOR)).map(String::trim).toList();
    }
}
