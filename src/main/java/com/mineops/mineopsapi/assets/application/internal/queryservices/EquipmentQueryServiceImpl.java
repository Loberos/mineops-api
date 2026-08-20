package com.mineops.mineopsapi.assets.application.internal.queryservices;

import com.mineops.mineopsapi.assets.domain.model.aggregates.Equipment;
import com.mineops.mineopsapi.assets.domain.model.queries.GetAllEquipmentQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetEquipmentByCodeQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetEquipmentByIdQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetFleetSummaryQuery;
import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;
import com.mineops.mineopsapi.assets.domain.model.valueobjects.FleetSummary;
import com.mineops.mineopsapi.assets.domain.services.EquipmentQueryService;
import com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories.EquipmentRepository;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;
import com.mineops.mineopsapi.shared.infrastructure.persistence.jpa.PageCriteriaTranslator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class EquipmentQueryServiceImpl implements EquipmentQueryService {

    private final EquipmentRepository equipmentRepository;

    public EquipmentQueryServiceImpl(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
    }

    @Override
    public PagedResult<Equipment> handle(GetAllEquipmentQuery query, PageCriteria criteria) {
        var page = findPage(query, PageCriteriaTranslator.toPageable(criteria));
        return PageCriteriaTranslator.toPagedResult(page, criteria);
    }

    /**
     * El listado completo se resuelve por el mismo camino que el paginado, pidiendo un tramo sin
     * límite. Así los filtros se aplican en un solo sitio y no hay dos consultas que puedan
     * discrepar en el orden o en el criterio.
     */
    @Override
    public List<Equipment> handle(GetAllEquipmentQuery query) {
        return handle(query, PageCriteria.unpaged()).content();
    }

    /**
     * Los filtros son opcionales e independientes, así que cada combinación se resuelve con su propia
     * consulta derivada. Eso mantiene el SQL generado libre del truco {@code (:param is null or ...)},
     * que anula el uso de índices y obliga al driver a adivinar el tipo de un enum nulo.
     */
    private Page<Equipment> findPage(GetAllEquipmentQuery query, Pageable pageable) {
        var status = query.status();
        var equipmentTypeId = query.equipmentTypeId();
        if (status != null && equipmentTypeId != null) {
            return equipmentRepository.findByStatusAndEquipmentTypeIdOrderByCodeAsc(
                    status, equipmentTypeId, pageable);
        }
        if (status != null) {
            return equipmentRepository.findByStatusOrderByCodeAsc(status, pageable);
        }
        if (equipmentTypeId != null) {
            return equipmentRepository.findByEquipmentTypeIdOrderByCodeAsc(equipmentTypeId, pageable);
        }
        return equipmentRepository.findAllByOrderByCodeAsc(pageable);
    }

    @Override
    public Optional<Equipment> handle(GetEquipmentByIdQuery query) {
        return equipmentRepository.findById(query.equipmentId());
    }

    @Override
    public Optional<Equipment> handle(GetEquipmentByCodeQuery query) {
        return equipmentRepository.findByCode(query.code() == null ? "" : query.code().trim().toUpperCase());
    }

    @Override
    public FleetSummary handle(GetFleetSummaryQuery query) {
        return new FleetSummary(
                equipmentRepository.count(),
                equipmentRepository.countByStatus(EquipmentStatus.BLOCKED),
                equipmentRepository.countAvailableNearThreshold());
    }
}
