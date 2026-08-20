package com.mineops.mineopsapi.workforce.interfaces.rest.transform;

import com.mineops.mineopsapi.workforce.domain.model.commands.GrantCertificationCommand;
import com.mineops.mineopsapi.workforce.interfaces.rest.resources.GrantCertificationResource;

public final class GrantCertificationCommandFromResourceAssembler {

    private GrantCertificationCommandFromResourceAssembler() {
    }

    public static GrantCertificationCommand toCommandFromResource(
            Long operatorId, GrantCertificationResource resource) {
        return new GrantCertificationCommand(
                operatorId, resource.equipmentTypeId(), resource.issuedOn(), resource.expiresOn());
    }
}
