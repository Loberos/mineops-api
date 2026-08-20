package com.mineops.mineopsapi.assets.application.internal.queryservices;

import com.mineops.mineopsapi.assets.domain.model.aggregates.MaintenanceRecord;
import com.mineops.mineopsapi.assets.domain.model.queries.GetMaintenanceHistoryQuery;
import com.mineops.mineopsapi.assets.domain.services.MaintenanceQueryService;
import com.mineops.mineopsapi.assets.infrastructure.persistence.jpa.repositories.MaintenanceRecordRepository;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;
import com.mineops.mineopsapi.shared.infrastructure.persistence.jpa.PageCriteriaTranslator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MaintenanceQueryServiceImpl implements MaintenanceQueryService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public MaintenanceQueryServiceImpl(MaintenanceRecordRepository maintenanceRecordRepository) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    @Override
    public PagedResult<MaintenanceRecord> handle(GetMaintenanceHistoryQuery query, PageCriteria criteria) {
        var pageable = PageCriteriaTranslator.toPageable(criteria);
        var page = query.equipmentId() == null
                ? maintenanceRecordRepository.findAllByOrderByPerformedOnDescIdDesc(pageable)
                : maintenanceRecordRepository.findByEquipmentIdOrderByPerformedOnDescIdDesc(
                        query.equipmentId(), pageable);
        return PageCriteriaTranslator.toPagedResult(page, criteria);
    }

    @Override
    public List<MaintenanceRecord> handle(GetMaintenanceHistoryQuery query) {
        return handle(query, PageCriteria.unpaged()).content();
    }
}
