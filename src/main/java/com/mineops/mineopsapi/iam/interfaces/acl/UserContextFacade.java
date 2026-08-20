package com.mineops.mineopsapi.iam.interfaces.acl;

import java.util.Optional;

/**
 * Capa anticorrupción que publica el contexto de identidad.
 * <p>
 * Los demás contextos resuelven quién autorizó una operación a través de esta fachada en lugar de
 * acceder al repositorio de usuarios, de modo que el agregado {@code User} nunca se filtra fuera de
 * su propia frontera.
 * </p>
 */
public interface UserContextFacade {

    /**
     * @param email usuario de acceso
     * @return el identificador del usuario, o vacío si nadie coincide
     */
    Optional<Long> fetchUserIdByEmail(String email);

    /**
     * @param userId identificador del usuario
     * @return el nombre visible que se usa en las trazas de auditoría, o vacío si nadie coincide
     */
    Optional<String> fetchFullNameByUserId(Long userId);

    /**
     * @param userId identificador del usuario
     * @return si el usuario tiene un rol autorizado a levantar una regla de negocio
     */
    boolean isAllowedToAuthorizeOverrides(Long userId);
}
