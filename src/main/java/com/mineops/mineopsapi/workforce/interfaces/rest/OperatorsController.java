package com.mineops.mineopsapi.workforce.interfaces.rest;

import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.interfaces.rest.resources.PagedResource;
import com.mineops.mineopsapi.workforce.domain.model.commands.ChangeOperatorStatusCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.RevokeCertificationCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.UpdateOperatorCommand;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetAllOperatorsQuery;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetWorkforceSummaryQuery;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetOperatorByIdQuery;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.OperatorStatus;
import com.mineops.mineopsapi.workforce.domain.services.OperatorCommandService;
import com.mineops.mineopsapi.workforce.domain.services.OperatorQueryService;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.ChangeOperatorStatusResource;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.CreateOperatorResource;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.GrantCertificationResource;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.OperatorResource;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.WorkforceSummaryResource;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.UpdateOperatorResource;
import com.mineops.mineopsapi.workforce.interfaces.rest.transform.CreateOperatorCommandFromResourceAssembler;
import com.mineops.mineopsapi.workforce.interfaces.rest.transform.GrantCertificationCommandFromResourceAssembler;
import com.mineops.mineopsapi.workforce.interfaces.rest.transform.OperatorResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/operators", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Operadores", description = "Plantilla y certificaciones que posee cada operador")
public class OperatorsController {

    private final OperatorCommandService operatorCommandService;
    private final OperatorQueryService operatorQueryService;

    public OperatorsController(
            OperatorCommandService operatorCommandService, OperatorQueryService operatorQueryService) {
        this.operatorCommandService = operatorCommandService;
        this.operatorQueryService = operatorQueryService;
    }

    @GetMapping
    @Operation(summary = "Listar operadores", description = "Paginados, y opcionalmente filtrados por estado")
    public ResponseEntity<PagedResource<OperatorResource>> getAllOperators(
            @RequestParam(required = false) OperatorStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var result = operatorQueryService
                .handle(new GetAllOperatorsQuery(status), PageCriteria.of(page, size))
                .map(OperatorResourceFromEntityAssembler::toResourceFromEntity);
        return ResponseEntity.ok(PagedResource.fromResult(result));
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Contadores de la dotación",
            description = "Medidos sobre la dotación entera, no sobre la página que se esté viendo")
    public ResponseEntity<WorkforceSummaryResource> getWorkforceSummary() {
        var summary = operatorQueryService.handle(new GetWorkforceSummaryQuery());
        return ResponseEntity.ok(WorkforceSummaryResource.fromSummary(summary));
    }

    @GetMapping("/{operatorId}")
    @Operation(summary = "Obtener un operador con sus certificaciones")
    public ResponseEntity<OperatorResource> getOperatorById(@PathVariable Long operatorId) {
        return operatorQueryService.handle(new GetOperatorByIdQuery(operatorId))
                .map(OperatorResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El operador", operatorId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Agregar un operador a la plantilla")
    public ResponseEntity<OperatorResource> createOperator(@Valid @RequestBody CreateOperatorResource resource) {
        var command = CreateOperatorCommandFromResourceAssembler.toCommandFromResource(resource);
        return operatorCommandService.handle(command)
                .map(OperatorResourceFromEntityAssembler::toResourceFromEntity)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PutMapping("/{operatorId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar el nombre de un operador")
    public ResponseEntity<OperatorResource> updateOperator(
            @PathVariable Long operatorId, @Valid @RequestBody UpdateOperatorResource resource) {
        var command = new UpdateOperatorCommand(operatorId, resource.firstName(), resource.lastName());
        return operatorCommandService.handle(command)
                .map(OperatorResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El operador", operatorId));
    }

    @PatchMapping("/{operatorId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Activar o desactivar a un operador")
    public ResponseEntity<OperatorResource> changeOperatorStatus(
            @PathVariable Long operatorId, @Valid @RequestBody ChangeOperatorStatusResource resource) {
        var command = new ChangeOperatorStatusCommand(operatorId, resource.status());
        return operatorCommandService.handle(command)
                .map(OperatorResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El operador", operatorId));
    }

    @PostMapping("/{operatorId}/certifications")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(
            summary = "Certificar a un operador para una familia de máquinas",
            description = "Enviarlo de nuevo para la misma familia renueva la certificación existente")
    public ResponseEntity<OperatorResource> grantCertification(
            @PathVariable Long operatorId, @Valid @RequestBody GrantCertificationResource resource) {
        var command = GrantCertificationCommandFromResourceAssembler.toCommandFromResource(operatorId, resource);
        return operatorCommandService.handle(command)
                .map(OperatorResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El operador", operatorId));
    }

    @DeleteMapping("/{operatorId}/certifications/{equipmentTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Revocar una certificación",
            description = "Para una certificación otorgada por error; una vencida se conserva como historial")
    public ResponseEntity<OperatorResource> revokeCertification(
            @PathVariable Long operatorId, @PathVariable Long equipmentTypeId) {
        var command = new RevokeCertificationCommand(operatorId, equipmentTypeId);
        return operatorCommandService.handle(command)
                .map(OperatorResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El operador", operatorId));
    }
}
