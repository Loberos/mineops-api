package com.mineops.mineopsapi.operations.interfaces.rest;

import com.mineops.mineopsapi.operations.domain.model.queries.GetMaintenanceProjectionQuery;
import com.mineops.mineopsapi.operations.domain.services.MaintenanceProjectionQueryService;
import com.mineops.mineopsapi.operations.infrastructure.configuration.OperationsProperties;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.ProjectedMaintenanceResource;
import com.mineops.mineopsapi.operations.interfaces.rest.transform.ProjectedMaintenanceResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Regla de negocio 12, expuesta como un modelo de lectura propio porque responde una pregunta sobre
 * el futuro y no sobre un registro en particular.
 */
@RestController
@RequestMapping(value = "/api/v1/maintenance-projection", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Proyección de mantenimiento",
        description = "Máquinas que alcanzarán su mantenimiento, según la programación")
public class MaintenanceProjectionController {

    private final MaintenanceProjectionQueryService maintenanceProjectionQueryService;
    private final OperationsProperties operationsProperties;

    public MaintenanceProjectionController(
            MaintenanceProjectionQueryService maintenanceProjectionQueryService,
            OperationsProperties operationsProperties) {
        this.maintenanceProjectionQueryService = maintenanceProjectionQueryService;
        this.operationsProperties = operationsProperties;
    }

    @GetMapping
    @Operation(
            summary = "Máquinas que alcanzan mantenimiento dentro del horizonte",
            description = "Recorre hora por hora los turnos ya programados e informa el turno en que cada "
                    + "máquina cruzaría su umbral. Las máquinas ya detenidas se listan primero")
    public ResponseEntity<List<ProjectedMaintenanceResource>> getProjection(
            @RequestParam(required = false) Integer horizonDays) {
        var horizon = horizonDays == null ? operationsProperties.projectionHorizonDays() : horizonDays;
        var resources = maintenanceProjectionQueryService.handle(new GetMaintenanceProjectionQuery(horizon)).stream()
                .map(ProjectedMaintenanceResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
