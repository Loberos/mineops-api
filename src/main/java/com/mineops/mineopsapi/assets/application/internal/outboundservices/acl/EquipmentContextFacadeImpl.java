package com.mineops.mineopsapi.assets.application.internal.outboundservices.acl;

import com.mineops.mineopsapi.assets.domain.model.commands.RegisterEquipmentUsageCommand;
import com.mineops.mineopsapi.assets.domain.model.queries.GetAllEquipmentQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetEquipmentByIdQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetEquipmentTypeByIdQuery;
import com.mineops.mineopsapi.assets.domain.services.EquipmentCommandService;
import com.mineops.mineopsapi.assets.domain.services.EquipmentQueryService;
import com.mineops.mineopsapi.assets.domain.services.EquipmentTypeQueryService;
import com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories.EquipmentRepository;
import com.mineops.mineopsapi.assets.interfaces.acl.EquipmentContextFacade;
import com.mineops.mineopsapi.assets.interfaces.acl.EquipmentSnapshot;
import com.mineops.mineopsapi.assets.interfaces.acl.EquipmentTypeSnapshot;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class EquipmentContextFacadeImpl implements EquipmentContextFacade {

    private final EquipmentQueryService equipmentQueryService;
    private final EquipmentTypeQueryService equipmentTypeQueryService;
    private final EquipmentCommandService equipmentCommandService;
    private final EquipmentRepository equipmentRepository;

    public EquipmentContextFacadeImpl(
            EquipmentQueryService equipmentQueryService,
            EquipmentTypeQueryService equipmentTypeQueryService,
            EquipmentCommandService equipmentCommandService,
            EquipmentRepository equipmentRepository) {
        this.equipmentQueryService = equipmentQueryService;
        this.equipmentTypeQueryService = equipmentTypeQueryService;
        this.equipmentCommandService = equipmentCommandService;
        this.equipmentRepository = equipmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EquipmentSnapshot> fetchEquipmentById(Long equipmentId) {
        return equipmentQueryService.handle(new GetEquipmentByIdQuery(equipmentId))
                .map(EquipmentSnapshot::fromAggregate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EquipmentTypeSnapshot> fetchEquipmentTypeById(Long equipmentTypeId) {
        return equipmentTypeQueryService.handle(new GetEquipmentTypeByIdQuery(equipmentTypeId))
                .map(EquipmentTypeSnapshot::fromAggregate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentSnapshot> fetchEquipmentByIds(List<Long> equipmentIds) {
        if (equipmentIds == null || equipmentIds.isEmpty()) {
            return List.of();
        }
        return equipmentRepository.findByIdInOrderByCodeAsc(equipmentIds).stream()
                .map(EquipmentSnapshot::fromAggregate)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentSnapshot> fetchAllEquipment() {
        return equipmentQueryService.handle(GetAllEquipmentQuery.unfiltered()).stream()
                .map(EquipmentSnapshot::fromAggregate)
                .toList();
    }

    @Override
    @Transactional
    public EquipmentSnapshot registerUsage(Long equipmentId, BigDecimal hours) {
        return equipmentCommandService.handle(new RegisterEquipmentUsageCommand(equipmentId, hours))
                .map(EquipmentSnapshot::fromAggregate)
                .orElseThrow(() -> new ResourceNotFoundException("El equipo", equipmentId));
    }
}
