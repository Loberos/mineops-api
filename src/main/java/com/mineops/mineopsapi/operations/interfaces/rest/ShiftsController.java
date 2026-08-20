package com.mineops.mineopsapi.operations.interfaces.rest;

import com.mineops.mineopsapi.operations.domain.model.commands.AssignOperatorToShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.CancelAssignmentCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.CancelShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.UpdateShiftPlanCommand;
import com.mineops.mineopsapi.operations.domain.model.queries.GetAllShiftsQuery;
import com.mineops.mineopsapi.operations.domain.model.queries.GetShiftByIdQuery;
import com.mineops.mineopsapi.operations.domain.model.queries.PreviewAssignmentQuery;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.ShiftStatus;
import com.mineops.mineopsapi.operations.domain.services.AssignmentCommandService;
import com.mineops.mineopsapi.operations.domain.services.AssignmentQueryService;
import com.mineops.mineopsapi.operations.domain.services.ShiftCommandService;
import com.mineops.mineopsapi.operations.domain.services.ShiftQueryService;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.AssignmentCreatedResource;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.AssignmentEvaluationResource;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.CancelReasonResource;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.CloseShiftResource;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.CreateAssignmentResource;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.CreateShiftResource;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.PreviewAssignmentResource;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.ShiftResource;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.UpdateShiftPlanResource;
import com.mineops.mineopsapi.operations.interfaces.rest.transform.AssignmentEvaluationResourceFromEntityAssembler;
import com.mineops.mineopsapi.operations.interfaces.rest.transform.AssignmentResourceFromEntityAssembler;
import com.mineops.mineopsapi.operations.interfaces.rest.transform.CloseShiftCommandFromResourceAssembler;
import com.mineops.mineopsapi.operations.interfaces.rest.transform.CreateShiftCommandFromResourceAssembler;
import com.mineops.mineopsapi.operations.interfaces.rest.transform.ShiftResourceFromEntityAssembler;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.interfaces.rest.resources.PagedResource;
import com.mineops.mineopsapi.shared.interfaces.rest.resources.RuleViolationResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/shifts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Turnos", description = "Programación, asignaciones y cierre de turnos")
public class ShiftsController {

    private final ShiftCommandService shiftCommandService;
    private final ShiftQueryService shiftQueryService;
    private final AssignmentCommandService assignmentCommandService;
    private final AssignmentQueryService assignmentQueryService;

    public ShiftsController(
            ShiftCommandService shiftCommandService,
            ShiftQueryService shiftQueryService,
            AssignmentCommandService assignmentCommandService,
            AssignmentQueryService assignmentQueryService) {
        this.shiftCommandService = shiftCommandService;
        this.shiftQueryService = shiftQueryService;
        this.assignmentCommandService = assignmentCommandService;
        this.assignmentQueryService = assignmentQueryService;
    }

    @GetMapping
    @Operation(
            summary = "Listar turnos",
            description = "Paginados, y opcionalmente acotados por rango de fechas y estado")
    public ResponseEntity<PagedResource<ShiftResource>> getAllShifts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ShiftStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        var result = shiftQueryService
                .handle(new GetAllShiftsQuery(from, to, status), PageCriteria.of(page, size))
                .map(ShiftResourceFromEntityAssembler::toResourceFromEntity);
        return ResponseEntity.ok(PagedResource.fromResult(result));
    }

    @GetMapping("/{shiftId}")
    @Operation(summary = "Obtener un turno con su dotación")
    public ResponseEntity<ShiftResource> getShiftById(@PathVariable Long shiftId) {
        return shiftQueryService.handle(new GetShiftByIdQuery(shiftId))
                .map(ShiftResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El turno", shiftId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'PLANNER')")
    @Operation(summary = "Programar un turno", description = "Un turno de día y uno de noche por día calendario")
    public ResponseEntity<ShiftResource> createShift(@Valid @RequestBody CreateShiftResource resource) {
        var command = CreateShiftCommandFromResourceAssembler.toCommandFromResource(resource);
        return shiftCommandService.handle(command)
                .map(ShiftResourceFromEntityAssembler::toResourceFromEntity)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PutMapping("/{shiftId}/plan")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'PLANNER')")
    @Operation(summary = "Replanificar un turno que no ha sido liquidado")
    public ResponseEntity<ShiftResource> updateShiftPlan(
            @PathVariable Long shiftId, @Valid @RequestBody UpdateShiftPlanResource resource) {
        var command = new UpdateShiftPlanCommand(shiftId, resource.plannedHours(), resource.notes());
        return shiftCommandService.handle(command)
                .map(ShiftResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El turno", shiftId));
    }

    @PostMapping("/{shiftId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Suspender un turno", description = "Libera todas sus asignaciones")
    public ResponseEntity<ShiftResource> cancelShift(
            @PathVariable Long shiftId, @Valid @RequestBody CancelReasonResource resource) {
        var command = new CancelShiftCommand(shiftId, resource.reason());
        return shiftCommandService.handle(command)
                .map(ShiftResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El turno", shiftId));
    }

    @PostMapping("/{shiftId}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(
            summary = "Liquidar un turno",
            description = "Registra las horas efectivamente trabajadas y las suma al horómetro de cada "
                    + "máquina, lo que puede llevar a alguna más allá de su umbral y bloquearla")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turno liquidado"),
            @ApiResponse(responseCode = "409", description = "El turno ya había sido liquidado"),
            @ApiResponse(
                    responseCode = "422",
                    description = "Horas fuera de rango, o una desviación del plan sin justificación")
    })
    public ResponseEntity<ShiftResource> closeShift(
            @PathVariable Long shiftId, @Valid @RequestBody CloseShiftResource resource) {
        var command = CloseShiftCommandFromResourceAssembler.toCommandFromResource(shiftId, resource);
        return shiftCommandService.handle(command)
                .map(ShiftResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El turno", shiftId));
    }

    @PostMapping("/{shiftId}/assignments/preview")
    @Operation(
            summary = "Verificar una asignación sin crearla",
            description = "Devuelve todas las reglas que la combinación incumpliría, para que el "
                    + "planificador las vea todas antes de enviar nada")
    public ResponseEntity<AssignmentEvaluationResource> previewAssignment(
            @PathVariable Long shiftId, @Valid @RequestBody PreviewAssignmentResource resource) {
        var evaluation = assignmentQueryService.handle(
                new PreviewAssignmentQuery(shiftId, resource.operatorId(), resource.equipmentId()));
        return ResponseEntity.ok(
                AssignmentEvaluationResourceFromEntityAssembler.toResourceFromEntity(evaluation));
    }

    @PostMapping("/{shiftId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'PLANNER')")
    @Operation(
            summary = "Asignar un operador y una máquina a un turno",
            description = "El rechazo informa todas las reglas incumplidas. Activar `force` pide que la "
                    + "asignación se haga de todos modos, algo que solo un supervisor puede hacer y que "
                    + "queda registrado junto con las reglas que se omitieron")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asignación creada"),
            @ApiResponse(responseCode = "403", description = "Solo un supervisor puede autorizar una excepción"),
            @ApiResponse(responseCode = "409", description = "Otro usuario tomó primero al operador o la máquina"),
            @ApiResponse(responseCode = "422", description = "La asignación incumple una o más reglas de negocio")
    })
    public ResponseEntity<AssignmentCreatedResource> createAssignment(
            @PathVariable Long shiftId,
            @Valid @RequestBody CreateAssignmentResource resource,
            @AuthenticationPrincipal UserDetails principal) {
        var command = new AssignOperatorToShiftCommand(
                shiftId,
                resource.operatorId(),
                resource.equipmentId(),
                resource.force(),
                resource.authorizationReason(),
                principal.getUsername());
        return assignmentCommandService.handle(command)
                .map(outcome -> new AssignmentCreatedResource(
                        AssignmentResourceFromEntityAssembler.toResourceFromEntity(outcome.assignment()),
                        outcome.warnings().stream().map(RuleViolationResource::fromViolation).toList()))
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/{shiftId}/assignments/{assignmentId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'PLANNER')")
    @Operation(
            summary = "Suspender una asignación",
            description = "Libera a su operador y a su máquina para poder programar un reemplazo")
    public ResponseEntity<ShiftResource> cancelAssignment(
            @PathVariable Long shiftId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody CancelReasonResource resource) {
        var command = new CancelAssignmentCommand(shiftId, assignmentId, resource.reason());
        return assignmentCommandService.handle(command)
                .map(ShiftResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("El turno", shiftId));
    }
}
