package com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.configuration;

import com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import com.mineops.mineopsapi.iam.infrastructure.authorization.sfs.pipeline.RestAuthenticationErrorHandler;
import com.mineops.mineopsapi.iam.infrastructure.hashing.bcrypt.BCryptHashingService;
import com.mineops.mineopsapi.iam.infrastructure.tokens.jwt.BearerTokenService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cableado de seguridad de la API.
 * <p>
 * La API es sin estado: no hay sesión ni token CSRF, porque cada petición lleva su propio token
 * bearer. La autorización se expresa junto a cada endpoint con {@code @PreAuthorize}, y esta cadena
 * solo declara qué rutas pueden alcanzarse sin autenticarse en absoluto.
 * </p>
 */
@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/authentication/sign-in",
            "/api/v1/authentication/sign-up",
            "/actuator/health",
            "/actuator/health/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };

    private final UserDetailsService userDetailsService;
    private final BearerTokenService tokenService;
    private final BCryptHashingService hashingService;
    private final RestAuthenticationErrorHandler authenticationErrorHandler;
    private final SecurityProperties securityProperties;

    public WebSecurityConfiguration(
            @Qualifier("defaultUserDetailsService") UserDetailsService userDetailsService,
            BearerTokenService tokenService,
            BCryptHashingService hashingService,
            RestAuthenticationErrorHandler authenticationErrorHandler,
            SecurityProperties securityProperties) {
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.hashingService = hashingService;
        this.authenticationErrorHandler = authenticationErrorHandler;
        this.securityProperties = securityProperties;
    }

    @Bean
    public BearerAuthorizationRequestFilter authorizationRequestFilter() {
        return new BearerAuthorizationRequestFilter(tokenService, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return hashingService;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        // Orígenes explícitos en vez de un comodín: la API la consume un frontend conocido.
        configuration.setAllowedOrigins(securityProperties.cors().allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Es seguro desactivarlo: la API no guarda cookie de sesión, así que no hay nada de
                // lo que una petición entre sitios pueda aprovecharse.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationErrorHandler)
                        .accessDeniedHandler(authenticationErrorHandler))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(authorizationRequestFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
