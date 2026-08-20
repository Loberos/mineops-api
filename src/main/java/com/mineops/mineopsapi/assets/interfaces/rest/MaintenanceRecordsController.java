package com.mineops.mineopsapi.assets.interfaces.rest;

import com.mineops.mineopsapi.assets.domain.model.queries.GetMaintenanceHistoryQuery;
import com.mineops.mineopsapi.assets.domain.services.MaintenanceQueryService;
import com.mineops.mineopsapi.assets.interfaces.rest.resources.MaintenanceRecordResource;
import com.mineops.mineopsapi.assets.interfaces.rest.transform.MaintenanceRecordResourceFromEntityAssembler;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.interfaces.rest.resources.PagedResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/maintenance-records", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Mantenimiento", description = "Historial de mantenimiento de toda la flota")
public class MaintenanceRecordsController {

    private final MaintenanceQueryService maintenanceQueryService;

    public MaintenanceRecordsController(MaintenanceQueryService maintenanceQueryService) {
        this.maintenanceQueryService = maintenanceQueryService;
    }

    @GetMapping
    @Operation(
            summary = "Historial de mantenimiento de la flota",
            description = "Paginado, del más reciente al más antiguo")
    public ResponseEntity<PagedResource<MaintenanceRecordResource>> getFleetMaintenanceHistory(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var result = maintenanceQueryService
                .handle(GetMaintenanceHistoryQuery.forFleet(), PageCriteria.of(page, size))
                .map(MaintenanceRecordResourceFromEntityAssembler::toResourceFromEntity);
        return ResponseEntity.ok(PagedResource.fromResult(result));
    }
}
