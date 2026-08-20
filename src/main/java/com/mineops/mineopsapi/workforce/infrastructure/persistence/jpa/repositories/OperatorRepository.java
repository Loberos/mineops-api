package com.mineops.mineopsapi.workforce.infrastructure.persistence.jpa.repositories;

import com.mineops.mineopsapi.workforce.domain.model.aggregates.Operator;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.OperatorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {

    Optional<Operator> findByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumber(String documentNumber);

    @Query(value = "select operator from Operator operator order by operator.name.lastName, operator.name.firstName",
            countQuery = "select count(operator) from Operator operator")
    Page<Operator> findAllOrderedByName(Pageable pageable);

    @Query(value = """
            select operator from Operator operator
            where operator.status = :status
            order by operator.name.lastName, operator.name.firstName
            """,
            countQuery = "select count(operator) from Operator operator where operator.status = :status")
    Page<Operator> findByStatusOrderedByName(@Param("status") OperatorStatus status, Pageable pageable);

    /**
     * Operadores que en la fecha indicada no tienen ninguna certificación vigente, y que por lo
     * tanto no pueden programarse.
     * <p>
     * Se resuelve con un {@code not exists} en vez de traer cada operador con sus certificaciones y
     * filtrarlos en memoria: es un conteo sobre la dotación completa, que es exactamente lo que la
     * paginación evita cargar.
     * </p>
     */
    @Query("""
            select count(operator) from Operator operator
            where not exists (
                select certification from Certification certification
                where certification.operator = operator
                  and certification.validity.issuedOn <= :on
                  and certification.validity.expiresOn >= :on
            )
            """)
    long countWithoutValidCertificationOn(@Param("on") LocalDate on);
}
