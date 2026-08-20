package com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories;

import com.mineops.mineopsapi.assets.domain.model.aggregates.EquipmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {

    Optional<EquipmentType> findByCode(String code);

    boolean existsByCode(String code);

    List<EquipmentType> findAllByOrderByNameAsc();
}
