package com.mineops.mineopsapi.workforce.infrastructure.persistence.jpa.repositories;

import com.mineops.mineopsapi.workforce.domain.model.entities.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

    /**
     * Certificaciones cuya ventana cierra en el día indicado o antes, de la más próxima a la más
     * lejana.
     * <p>
     * El operador se trae junto con ellas porque todo llamador muestra el nombre de la persona, y
     * hacerlo en una sola sentencia evita una consulta por fila.
     * </p>
     */
    @Query("""
            select certification from Certification certification
            join fetch certification.operator
            where certification.validity.expiresOn <= :limit
            order by certification.validity.expiresOn asc
            """)
    List<Certification> findExpiringOnOrBefore(@Param("limit") LocalDate limit);
}
