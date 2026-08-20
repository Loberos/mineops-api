package com.mineops.mineopsapi.operations.interfaces.rest;

import com.mineops.mineopsapi.operations.domain.model.queries.GetAssignmentsAtRiskQuery;
import com.mineops.mineopsapi.operations.domain.services.AssignmentQueryService;
import com.mineops.mineopsapi.operations.interfaces.rest.resources.AssignmentAtRiskResource;
import com.mineops.mineopsapi.operations.interfaces.rest.transform.AssignmentAtRiskResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/assignments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Asignaciones", description = "Asignaciones a lo largo de toda la programación")
public class AssignmentsController {

    private final AssignmentQueryService assignmentQueryService;

    public AssignmentsController(AssignmentQueryService assignmentQueryService) {
        this.assignmentQueryService = assignmentQueryService;
    }

    @GetMapping("/at-risk")
    @Operation(
            summary = "Asignaciones que esperan una decisión",
            description = "Asignaciones que eran válidas al planificarse y ya no lo son, casi siempre porque "
                    + "su máquina se bloqueó después. Es la lista de trabajo del planificador")
    public ResponseEntity<List<AssignmentAtRiskResource>> getAssignmentsAtRisk() {
        var resources = assignmentQueryService.handle(new GetAssignmentsAtRiskQuery()).stream()
                .map(AssignmentAtRiskResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
