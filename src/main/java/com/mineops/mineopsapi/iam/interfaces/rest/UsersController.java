package com.mineops.mineopsapi.iam.interfaces.rest;

import com.mineops.mineopsapi.iam.domain.model.queries.GetAllUsersQuery;
import com.mineops.mineopsapi.iam.domain.model.queries.GetUserByEmailQuery;
import com.mineops.mineopsapi.iam.domain.model.queries.GetUserByIdQuery;
import com.mineops.mineopsapi.iam.domain.services.UserQueryService;
import com.mineops.mineopsapi.iam.interfaces.rest.resources.UserResource;
import com.mineops.mineopsapi.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.interfaces.rest.resources.PagedResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Usuarios", description = "Usuarios de la plataforma")
public class UsersController {

    private final UserQueryService userQueryService;

    public UsersController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario actual", description = "Devuelve el usuario dueño del token bearer")
    public ResponseEntity<UserResource> getCurrentUser(@AuthenticationPrincipal UserDetails principal) {
        return userQueryService.handle(new GetUserByEmailQuery(principal.getUsername()))
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario", principal.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuarios", description = "Paginados, por correo")
    public ResponseEntity<PagedResource<UserResource>> getAllUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var result = userQueryService
                .handle(new GetAllUsersQuery(), PageCriteria.of(page, size))
                .map(UserResourceFromEntityAssembler::toResourceFromEntity);
        return ResponseEntity.ok(PagedResource.fromResult(result));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener un usuario por su identificador")
    public ResponseEntity<UserResource> getUserById(@PathVariable Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario", userId));
    }
}
