package com.mineops.mineopsapi.iam.interfaces.rest;

import com.mineops.mineopsapi.iam.domain.services.UserCommandService;
import com.mineops.mineopsapi.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.mineops.mineopsapi.iam.interfaces.rest.resources.SignInResource;
import com.mineops.mineopsapi.iam.interfaces.rest.resources.SignUpResource;
import com.mineops.mineopsapi.iam.interfaces.rest.resources.UserResource;
import com.mineops.mineopsapi.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import com.mineops.mineopsapi.iam.interfaces.rest.transform.SignInCommandFromResourceAssembler;
import com.mineops.mineopsapi.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import com.mineops.mineopsapi.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Autenticación", description = "Inicio de sesión y registro")
@SecurityRequirements
public class AuthenticationController {

    private final UserCommandService userCommandService;

    public AuthenticationController(UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Iniciar sesión", description = "Verifica las credenciales y devuelve un token bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<AuthenticatedUserResource> signIn(@Valid @RequestBody SignInResource resource) {
        var command = SignInCommandFromResourceAssembler.toCommandFromResource(resource);
        return userCommandService.handle(command)
                .map(AuthenticatedUserResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/sign-up")
    @Operation(summary = "Registrarse", description = "Registra un nuevo usuario de la plataforma")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado"),
            @ApiResponse(responseCode = "409", description = "El correo ya está en uso")
    })
    public ResponseEntity<UserResource> signUp(@Valid @RequestBody SignUpResource resource) {
        var command = SignUpCommandFromResourceAssembler.toCommandFromResource(resource);
        return userCommandService.handle(command)
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }
}
