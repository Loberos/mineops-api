package com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories;

import com.mineops.mineopsapi.assets.domain.model.aggregates.MaintenanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    Page<MaintenanceRecord> findByEquipmentIdOrderByPerformedOnDescIdDesc(Long equipmentId, Pageable pageable);

    Page<MaintenanceRecord> findAllByOrderByPerformedOnDescIdDesc(Pageable pageable);
}
