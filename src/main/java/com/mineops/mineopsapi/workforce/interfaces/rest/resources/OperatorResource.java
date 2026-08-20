package com.mineops.mineopsapi.workforce.interfaces.rest.resources;

import java.util.List;

public record OperatorResource(
        Long id,
        String documentNumber,
        String firstName,
        String lastName,
        String fullName,
        String status,
        boolean availableForAssignment,
        List<CertificationResource> certifications) {
}
