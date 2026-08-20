package com.mineops.mineopsapi.iam.domain.model.aggregates;

import com.mineops.mineopsapi.iam.domain.model.entities.Role;
import com.mineops.mineopsapi.iam.domain.model.valueobjects.Roles;
import com.mineops.mineopsapi.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persona que opera la plataforma: un planificador, un supervisor o un administrador.
 * <p>
 * Este agregado se mantiene deliberadamente separado del {@code Operator} del contexto de personal.
 * Un usuario es alguien que <em>usa el sistema</em>; un operador es alguien que <em>conduce una
 * máquina</em>. Tienen ciclos de vida distintos y solo unas pocas personas son ambas cosas.
 * </p>
 */
@Entity
@Table(name = "users")
@Getter
public class User extends AuditableAbstractAggregateRoot<User> {

    @NotBlank
    @Email
    @Size(max = 160)
    @Column(name = "email", length = 160, nullable = false, unique = true)
    private String email;

    /** Siempre un hash BCrypt. La contraseña en claro nunca llega a este agregado. */
    @NotBlank
    @Size(max = 120)
    @Column(name = "password", length = 120, nullable = false)
    private String password;

    @NotBlank
    @Size(max = 120)
    @Column(name = "full_name", length = 120, nullable = false)
    private String fullName;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    protected User() {
        // Requerido por JPA.
    }

    public User(String email, String hashedPassword, String fullName) {
        this.email = email;
        this.password = hashedPassword;
        this.fullName = fullName;
        this.active = true;
        this.roles = new HashSet<>();
    }

    public User(String email, String hashedPassword, String fullName, List<Role> roles) {
        this(email, hashedPassword, fullName);
        addRoles(roles);
    }

    public User addRole(Role role) {
        this.roles.add(role);
        return this;
    }

    public User addRoles(List<Role> roles) {
        this.roles.addAll(Role.validateRoleSet(roles));
        return this;
    }

    /**
     * Indica si este usuario puede autorizar una asignación que incumple una regla de negocio.
     */
    public boolean canAuthorizeOverrides() {
        return hasAnyRole(Roles.ROLE_SUPERVISOR, Roles.ROLE_ADMIN);
    }

    public boolean hasAnyRole(Roles... candidates) {
        return this.roles.stream()
                .map(Role::getName)
                .anyMatch(granted -> List.of(candidates).contains(granted));
    }

    public void deactivate() {
        this.active = false;
    }
}
