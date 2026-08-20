package com.mineops.mineopsapi.iam.domain.model.entities;

import com.mineops.mineopsapi.iam.domain.model.valueobjects.Roles;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Rol que puede otorgarse a un usuario.
 * <p>
 * El catálogo es cerrado: solo existen filas para los miembros de {@link Roles} y se siembran una
 * sola vez al arrancar, lo que mantiene las decisiones de autorización en el código y fuera de la
 * base de datos.
 * </p>
 */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", length = 60, nullable = false, unique = true)
    private Roles name;

    public Role(Roles name) {
        this.name = name;
    }

    public String getStringName() {
        return name.name();
    }

    public static Role getDefaultRole() {
        return new Role(Roles.getDefaultRole());
    }

    /**
     * Construye un rol a partir de su nombre textual, aceptando tanto {@code ROLE_SUPERVISOR} como
     * {@code SUPERVISOR} para no obligar al cliente a conocer el prefijo.
     *
     * @param name nombre del rol
     * @return el rol correspondiente
     * @throws IllegalArgumentException si ningún rol coincide con el nombre recibido
     */
    public static Role toRoleFromName(String name) {
        var normalized = name == null ? "" : name.trim().toUpperCase();
        var prefixed = normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
        try {
            return new Role(Roles.valueOf(prefixed));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("%s no es un rol válido".formatted(name), exception);
        }
    }

    /**
     * Cae al rol por defecto cuando no se solicitó ninguno.
     *
     * @param roles los roles solicitados, posiblemente vacíos o nulos
     * @return los roles a otorgar, nunca vacío
     */
    public static List<Role> validateRoleSet(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of(getDefaultRole());
        }
        return roles;
    }
}
