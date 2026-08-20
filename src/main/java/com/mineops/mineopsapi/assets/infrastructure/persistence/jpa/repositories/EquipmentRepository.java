package com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories;

import com.mineops.mineopsapi.assets.domain.model.aggregates.Equipment;
import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByCode(String code);

    boolean existsByCode(String code);

    Page<Equipment> findAllByOrderByCodeAsc(Pageable pageable);

    Page<Equipment> findByStatusOrderByCodeAsc(EquipmentStatus status, Pageable pageable);

    Page<Equipment> findByEquipmentTypeIdOrderByCodeAsc(Long equipmentTypeId, Pageable pageable);

    Page<Equipment> findByStatusAndEquipmentTypeIdOrderByCodeAsc(
            EquipmentStatus status, Long equipmentTypeId, Pageable pageable);

    List<Equipment> findByIdInOrderByCodeAsc(List<Long> equipmentIds);

    boolean existsByEquipmentTypeId(Long equipmentTypeId);

    long countByStatus(EquipmentStatus status);

    /**
     * Máquinas disponibles a las que les queda menos del diez por ciento de su ciclo.
     * <p>
     * El margen restante es la diferencia entre el umbral y el horómetro, la misma que calcula
     * {@code Equipment.hoursUntilMaintenance()}. Se repite aquí en JPQL, y no se resuelve
     * recorriendo agregados, porque es un conteo sobre toda la flota y traerla entera a memoria
     * para filtrarla sería justo lo que la paginación viene a evitar.
     * </p>
     */
    @Query("""
            select count(equipment) from Equipment equipment
            where equipment.status = com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus.AVAILABLE
              and (equipment.maintenanceThresholdHours - equipment.hourMeter)
                  <= equipment.equipmentType.maintenanceIntervalHours * 0.1
            """)
    long countAvailableNearThreshold();
}
