package com.mineops.mineopsapi.iam.interfaces.rest;

import com.mineops.mineopsapi.iam.domain.model.queries.GetAllRolesQuery;
import com.mineops.mineopsapi.iam.domain.services.RoleQueryService;
import com.mineops.mineopsapi.iam.interfaces.rest.resources.RoleResource;
import com.mineops.mineopsapi.iam.interfaces.rest.transform.RoleResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/roles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Roles", description = "Catálogo de roles")
public class RolesController {

    private final RoleQueryService roleQueryService;

    public RolesController(RoleQueryService roleQueryService) {
        this.roleQueryService = roleQueryService;
    }

    @GetMapping
    @Operation(summary = "Listar roles")
    public ResponseEntity<List<RoleResource>> getAllRoles() {
        var roles = roleQueryService.handle(new GetAllRolesQuery()).stream()
                .map(RoleResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(roles);
    }
}
