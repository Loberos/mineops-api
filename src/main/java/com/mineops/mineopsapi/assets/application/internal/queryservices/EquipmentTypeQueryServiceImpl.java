package com.mineops.mineopsapi.assets.application.internal.queryservices;

import com.mineops.mineopsapi.assets.domain.model.aggregates.EquipmentType;
import com.mineops.mineopsapi.assets.domain.model.queries.GetAllEquipmentTypesQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetEquipmentTypeByIdQuery;
import com.mineops.mineopsapi.assets.domain.services.EquipmentTypeQueryService;
import com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories.EquipmentTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class EquipmentTypeQueryServiceImpl implements EquipmentTypeQueryService {

    private final EquipmentTypeRepository equipmentTypeRepository;

    public EquipmentTypeQueryServiceImpl(EquipmentTypeRepository equipmentTypeRepository) {
        this.equipmentTypeRepository = equipmentTypeRepository;
    }

    @Override
    public List<EquipmentType> handle(GetAllEquipmentTypesQuery query) {
        return equipmentTypeRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Optional<EquipmentType> handle(GetEquipmentTypeByIdQuery query) {
        return equipmentTypeRepository.findById(query.equipmentTypeId());
    }
}
