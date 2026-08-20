package com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.model;

import com.mineops.mineopsapi.iam.domain.model.aggregates.User;
import com.mineops.mineopsapi.iam.domain.model.entities.Role;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapta el agregado {@link User} al contrato que espera Spring Security, manteniendo las
 * anotaciones del framework fuera del modelo de dominio.
 */
@Getter
@EqualsAndHashCode
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String username;
    private final String fullName;

    @EqualsAndHashCode.Exclude
    private final String password;

    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserDetailsImpl(
            Long id,
            String username,
            String fullName,
            String password,
            boolean enabled,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        var authorities = user.getRoles().stream()
                .map(Role::getStringName)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPassword(),
                user.isActive(),
                authorities);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Nombres de los roles otorgados, usados para construir la respuesta de autenticación.
     */
    public List<String> getRoleNames() {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }
}
