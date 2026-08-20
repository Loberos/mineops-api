package com.mineops.mineopsapi.operations.infrastructure.persistence.jpa.repositories;

import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.operations.domain.model.entities.Assignment;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentStatus;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.Journey;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.ShiftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio del agregado de turno. A las asignaciones se llega a través de su turno, porque forman
 * parte de él; la única excepción es la búsqueda de solo lectura que arma las listas de trabajo.
 */
@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    Optional<Shift> findByDateAndJourney(LocalDate date, Journey journey);

    boolean existsByDateAndJourney(LocalDate date, Journey journey);

    // El conteo de cada listado se declara aparte en vez de dejar que Spring Data lo derive: la
    // derivación reescribe la consulta quitándole el orden, y basta que una de estas gane un join
    // para que el conteo derivado empiece a contar filas repetidas en silencio.

    @Query(value = "select shift from Shift shift order by shift.date desc, shift.journey",
            countQuery = "select count(shift) from Shift shift")
    Page<Shift> findAllOrdered(Pageable pageable);

    @Query(value = """
            select shift from Shift shift
            where shift.date between :from and :to
            order by shift.date, shift.journey
            """,
            countQuery = "select count(shift) from Shift shift where shift.date between :from and :to")
    Page<Shift> findByDateRangeOrdered(
            @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Query(value = "select shift from Shift shift where shift.status = :status order by shift.date desc, shift.journey",
            countQuery = "select count(shift) from Shift shift where shift.status = :status")
    Page<Shift> findByStatusOrdered(@Param("status") ShiftStatus status, Pageable pageable);

    @Query(value = """
            select shift from Shift shift
            where shift.date between :from and :to and shift.status = :status
            order by shift.date, shift.journey
            """,
            countQuery = """
            select count(shift) from Shift shift
            where shift.date between :from and :to and shift.status = :status
            """)
    Page<Shift> findByDateRangeAndStatusOrdered(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") ShiftStatus status,
            Pageable pageable);

    /**
     * Los turnos que alimentan la proyección de mantenimiento: aún programados, dentro del horizonte y
     * leídos en orden cronológico porque la proyección acumula horas turno a turno.
     */
    @Query("""
            select distinct shift from Shift shift
            left join fetch shift.assignments
            where shift.status = com.mineops.mineopsapi.operations.domain.model.valueobjects.ShiftStatus.PLANNED
              and shift.date between :from and :to
            order by shift.date, shift.journey
            """)
    List<Shift> findPlannedWithAssignmentsInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Asignaciones en un estado dado a lo largo de todos los turnos. Es de solo lectura: las
     * asignaciones se siguen escribiendo a través de su turno.
     */
    @Query("""
            select assignment from Assignment assignment
            join fetch assignment.shift shift
            where assignment.status = :status
            order by shift.date, shift.journey
            """)
    List<Assignment> findAssignmentsByStatus(@Param("status") AssignmentStatus status);

    /**
     * Los turnos que todavía están por delante y tienen una asignación abierta con la máquina
     * indicada. Se usa al bloquear una máquina, para marcar la dotación que ya se había armado con
     * ella.
     */
    @Query("""
            select distinct shift from Shift shift
            join shift.assignments assignment
            where shift.status = com.mineops.mineopsapi.operations.domain.model.valueobjects.ShiftStatus.PLANNED
              and shift.date >= :from
              and assignment.equipmentId = :equipmentId
            order by shift.date, shift.journey
            """)
    List<Shift> findPlannedShiftsWithEquipment(
            @Param("equipmentId") Long equipmentId, @Param("from") LocalDate from);
}
