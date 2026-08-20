package com.mineops.mineopsapi.shared.infrastructure.persistence.jpa.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activa el listener de auditoría que rellena las columnas {@code created_at} y {@code updated_at}
 * declaradas por el agregado raíz auditable.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {
}
