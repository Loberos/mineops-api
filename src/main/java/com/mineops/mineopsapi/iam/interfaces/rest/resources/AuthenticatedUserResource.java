package com.mineops.mineopsapi.iam.interfaces.rest.resources;

import java.util.List;

public record AuthenticatedUserResource(
        Long id, String email, String fullName, List<String> roles, String token) {
}
