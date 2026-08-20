package com.mineops.mineopsapi.assets.interfaces.rest;

import com.mineops.mineopsapi.assets.domain.model.commands.ChangeEquipmentStatusCommand;
import com.mineops.mineopsapi.assets.domain.model.queries.GetAllEquipmentQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetEquipmentByIdQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetFleetSummaryQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetMaintenanceHistoryQuery;
import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;
import com.mineops.mineopsapi.assets.domain.services.EquipmentCommandService;
import com.mineops.mineopsapi.assets.domain.services.EquipmentQueryService;
import com.mineops.mineopsapi.assets.domain.services.MaintenanceCommandService;
import com.mineops.mineopsapi.assets.domain.services.MaintenanceQueryService;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.ChangeEquipmentStatusResource;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.CreateEquipmentResource;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.EquipmentResource;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.FleetSummaryResource;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.MaintenanceRecordResource;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.RegisterMaintenanceResource;
import com.mineops.mineopsapi.assets.interfaces.rest.transform.CreateEquipmentCommandFromResourceAssembler;
import com.mineops.mineopsapi.assets.interfaces.rest.transform.EquipmentResourceFromEntityAssembler;
import com.mineops.mineopsapi.assets.interfaces.rest.transform.MaintenanceRecordResourceFromEntityAssembler;
import com.mineops.mineopsapi.assets.interfaces.rest.transform.RegisterMaintenanceCommandFromResourceAssembler;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.interfaces.rest.resources.PagedResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/equipment", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Equipos", description = "Flota, horómetros e historial de mantenimiento")
public class EquipmentController {

    private final EquipmentCommandService equipmentCommandService;
    private final EquipmentQueryService equipmentQueryService;
    private final MaintenanceCommandService maintenanceCommandService;
    private final MaintenanceQueryService maintenanceQueryService;

    public EquipmentController(
            EquipmentCommandService equipmentCommandService,
            EquipmentQueryService equipmentQueryService,
            MaintenanceCommandService maintenanceCommandService,
            MaintenanceQueryService maintenanceQueryService) {
        this.equipmentCommandService = equipmentCommandService;
        this.equipmentQueryService = equipmentQueryService;
        this.maintenanceCommandService = maintenanceCommandService;
        this.maintenanceQueryService = maintenanceQueryService;
    }

    @GetMapping
    @Operation(
            summary = "Listar la flota",
            description = "Paginada, y opcionalmente filtrada por estado y por familia")
    public ResponseEntity<PagedResource<EquipmentResource>> getAllEquipment(
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) Long equipmentTypeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var result = equipmentQueryService
                .handle(new GetAllEquipmentQuery(status, equipmentTypeId), PageCriteria.of(page, size))
                .map(EquipmentResourceFromEntityAssembler::toResourceFromEntity);
        return ResponseEntity.ok(PagedResource.fromResult(result));
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Contadores de la flota",
            description = "Medidos sobre la flota entera, no sobre la página que se esté viendo")
    public ResponseEntity<FleetSummaryResource> getFleetSummary() {
        var summary = equipmentQueryService.handle(new GetFleetSummaryQuery());
        return ResponseEntity.ok(FleetSummaryResource.fromSummary(summary));
    }

    @GetMapping("/{equipmentId}")
    @Operation(summary = "Obtener una máquina")
    public ResponseEntity<EquipmentResource> getEquipmentById(@PathVariable Long equipmentId) {
        return equipmentQueryService.handle(new GetEquipmentByIdQuery(equipmentId))
                .map(EquipmentResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El equipo", equipmentId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar una máquina en la flota")
    public ResponseEntity<EquipmentResource> createEquipment(@Valid @RequestBody CreateEquipmentResource resource) {
        var command = CreateEquipmentCommandFromResourceAssembler.toCommandFromResource(resource);
        return equipmentCommandService.handle(command)
                .map(EquipmentResourceFromEntityAssembler::toResourceFromEntity)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PatchMapping("/{equipmentId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(
            summary = "Cambiar el estado de una máquina",
            description = "Una máquina bloqueada solo puede liberarse registrando su mantenimiento")
    public ResponseEntity<EquipmentResource> changeEquipmentStatus(
            @PathVariable Long equipmentId, @Valid @RequestBody ChangeEquipmentStatusResource resource) {
        var command = new ChangeEquipmentStatusCommand(equipmentId, resource.status());
        return equipmentCommandService.handle(command)
                .map(EquipmentResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El equipo", equipmentId));
    }

    @GetMapping("/{equipmentId}/maintenance-records")
    @Operation(
            summary = "Historial de mantenimiento de una máquina",
            description = "Paginado, del más reciente al más antiguo")
    public ResponseEntity<PagedResource<MaintenanceRecordResource>> getMaintenanceHistory(
            @PathVariable Long equipmentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var result = maintenanceQueryService
                .handle(new GetMaintenanceHistoryQuery(equipmentId), PageCriteria.of(page, size))
                .map(MaintenanceRecordResourceFromEntityAssembler::toResourceFromEntity);
        return ResponseEntity.ok(PagedResource.fromResult(result));
    }

    @PostMapping("/{equipmentId}/maintenance-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'PLANNER')")
    @Operation(
            summary = "Registrar un mantenimiento",
            description = "Libera la máquina, abre su siguiente ciclo y escribe la entrada de historial")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mantenimiento registrado y máquina liberada"),
            @ApiResponse(responseCode = "400", description = "La lectura es menor al horómetro actual")
    })
    public ResponseEntity<MaintenanceRecordResource> registerMaintenance(
            @PathVariable Long equipmentId, @Valid @RequestBody RegisterMaintenanceResource resource) {
        var command = RegisterMaintenanceCommandFromResourceAssembler.toCommandFromResource(equipmentId, resource);
        return maintenanceCommandService.handle(command)
                .map(MaintenanceRecordResourceFromEntityAssembler::toResourceFromEntity)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }
}
