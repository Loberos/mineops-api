package com.mineops.mineopsapi.shared.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Clase base de todo agregado raíz del sistema.
 * <p>
 * Aporta el identificador subrogado y las marcas de auditoría, y hereda de
 * {@link AbstractAggregateRoot} la capacidad de registrar eventos de dominio, de modo que un
 * agregado puede publicar lo que le ocurrió sin depender de la infraestructura que despacha esos
 * eventos.
 * </p>
 *
 * @param <T> el tipo concreto del agregado raíz
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class AuditableAbstractAggregateRoot<T extends AbstractAggregateRoot<T>>
        extends AbstractAggregateRoot<T> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
