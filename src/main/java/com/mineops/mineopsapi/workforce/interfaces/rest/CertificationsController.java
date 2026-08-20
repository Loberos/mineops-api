package com.mineops.mineopsapi.workforce.interfaces.rest;

import com.mineops.mineopsapi.workforce.domain.model.queries.GetExpiringCertificationsQuery;
import com.mineops.mineopsapi.workforce.domain.services.OperatorQueryService;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.ExpiringCertificationResource;
import com.mineops.mineopsapi.workforce.interfaces.rest.transform.ExpiringCertificationResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/certifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Certificaciones", description = "Certificaciones de toda la plantilla")
public class CertificationsController {

    private static final int DEFAULT_HORIZON_DAYS = 30;

    private final OperatorQueryService operatorQueryService;

    public CertificationsController(OperatorQueryService operatorQueryService) {
        this.operatorQueryService = operatorQueryService;
    }

    @GetMapping("/expiring")
    @Operation(
            summary = "Certificaciones próximas a vencer",
            description = "De la más próxima a la más lejana. Se incluyen las ya vencidas, porque son las "
                    + "que impiden a un operador trabajar hoy")
    public ResponseEntity<List<ExpiringCertificationResource>> getExpiringCertifications(
            @RequestParam(defaultValue = "" + DEFAULT_HORIZON_DAYS) int withinDays) {
        var resources = operatorQueryService.handle(new GetExpiringCertificationsQuery(withinDays)).stream()
                .map(ExpiringCertificationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
