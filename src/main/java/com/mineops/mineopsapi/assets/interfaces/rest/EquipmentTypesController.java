package com.mineops.mineopsapi.assets.interfaces.rest;

import com.mineops.mineopsapi.assets.domain.model.queries.GetAllEquipmentTypesQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetEquipmentTypeByIdQuery;
import com.mineops.mineopsapi.assets.domain.services.EquipmentTypeCommandService;
import com.mineops.mineopsapi.assets.domain.services.EquipmentTypeQueryService;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.CreateEquipmentTypeResource;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.EquipmentTypeResource;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.UpdateEquipmentTypeResource;
import com.mineops.mineopsapi.assets.interfaces.rest.transform.CreateEquipmentTypeCommandFromResourceAssembler;
import com.mineops.mineopsapi.assets.interfaces.rest.transform.EquipmentTypeResourceFromEntityAssembler;
import com.mineops.mineopsapi.assets.interfaces.rest.transform.UpdateEquipmentTypeCommandFromResourceAssembler;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/equipment-types", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tipos de equipo", description = "Familias de máquinas y su intervalo de mantenimiento")
public class EquipmentTypesController {

    private final EquipmentTypeCommandService equipmentTypeCommandService;
    private final EquipmentTypeQueryService equipmentTypeQueryService;

    public EquipmentTypesController(
            EquipmentTypeCommandService equipmentTypeCommandService,
            EquipmentTypeQueryService equipmentTypeQueryService) {
        this.equipmentTypeCommandService = equipmentTypeCommandService;
        this.equipmentTypeQueryService = equipmentTypeQueryService;
    }

    @GetMapping
    @Operation(summary = "Listar tipos de equipo")
    public ResponseEntity<List<EquipmentTypeResource>> getAllEquipmentTypes() {
        var resources = equipmentTypeQueryService.handle(new GetAllEquipmentTypesQuery()).stream()
                .map(EquipmentTypeResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{equipmentTypeId}")
    @Operation(summary = "Obtener un tipo de equipo")
    public ResponseEntity<EquipmentTypeResource> getEquipmentTypeById(@PathVariable Long equipmentTypeId) {
        return equipmentTypeQueryService.handle(new GetEquipmentTypeByIdQuery(equipmentTypeId))
                .map(EquipmentTypeResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El tipo de equipo", equipmentTypeId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un tipo de equipo")
    public ResponseEntity<EquipmentTypeResource> createEquipmentType(
            @Valid @RequestBody CreateEquipmentTypeResource resource) {
        var command = CreateEquipmentTypeCommandFromResourceAssembler.toCommandFromResource(resource);
        return equipmentTypeCommandService.handle(command)
                .map(EquipmentTypeResourceFromEntityAssembler::toResourceFromEntity)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PutMapping("/{equipmentTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Actualizar un tipo de equipo",
            description = "Un nuevo intervalo solo aplica a los ciclos que se abran de aquí en adelante")
    public ResponseEntity<EquipmentTypeResource> updateEquipmentType(
            @PathVariable Long equipmentTypeId, @Valid @RequestBody UpdateEquipmentTypeResource resource) {
        var command =
                UpdateEquipmentTypeCommandFromResourceAssembler.toCommandFromResource(equipmentTypeId, resource);
        return equipmentTypeCommandService.handle(command)
                .map(EquipmentTypeResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El tipo de equipo", equipmentTypeId));
    }
}
