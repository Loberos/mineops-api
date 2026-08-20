package com.mineops.mineopsapi.iam.domain.model.valueobjects;

/**
 * Roles que reconoce la plataforma, del menos al más privilegiado.
 * <p>
 * Se corresponden directamente con las responsabilidades de la operación minera: un planificador
 * arma la programación, un supervisor es el único que puede autorizar una asignación que incumple
 * reglas y cerrar turnos, y un administrador además mantiene los catálogos.
 * </p>
 */
public enum Roles {

    /** Acceso de solo lectura a toda la operación. */
    ROLE_VIEWER,

    /** Crea turnos y asignaciones, y registra mantenimientos. */
    ROLE_PLANNER,

    /** Todo lo del planificador, más cerrar turnos y autorizar asignaciones forzadas. */
    ROLE_SUPERVISOR,

    /** Todo lo del supervisor, más administrar usuarios, tipos de equipo y operadores. */
    ROLE_ADMIN;

    /**
     * Rol que se otorga a quien se registra sin pedir nada más.
     */
    public static Roles getDefaultRole() {
        return ROLE_VIEWER;
    }
}
