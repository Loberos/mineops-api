package com.mineops.mineopsapi.assets.domain.services;

import com.mineops.mineopsapi.assets.domain.model.aggregates.EquipmentType;
import com.mineops.mineopsapi.assets.domain.model.queries.GetAllEquipmentTypesQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetEquipmentTypeByIdQuery;

import java.util.List;
import java.util.Optional;

public interface EquipmentTypeQueryService {

    List<EquipmentType> handle(GetAllEquipmentTypesQuery query);

    Optional<EquipmentType> handle(GetEquipmentTypeByIdQuery query);
}
