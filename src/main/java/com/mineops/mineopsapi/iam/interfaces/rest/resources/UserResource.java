package com.mineops.mineopsapi.iam.interfaces.rest.resources;

import java.util.List;

public record UserResource(Long id, String email, String fullName, boolean active, List<String> roles) {
}
