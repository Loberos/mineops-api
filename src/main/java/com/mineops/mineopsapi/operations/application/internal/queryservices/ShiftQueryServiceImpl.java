package com.mineops.mineopsapi.operations.application.internal.queryservices;

import com.mineops.mineopsapi.operations.domain.model.aggregates.Shift;
import com.mineops.mineopsapi.operations.domain.model.queries.GetAllShiftsQuery;
import com.mineops.mineopsapi.operations.domain.model.queries.GetShiftByIdQuery;
import com.mineops.mineopsapi.operations.domain.services.ShiftQueryService;
import com.mineops.mineopsapi.operations.infrastructure.persistence.jpa.repositories.ShiftRepository;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;
import com.mineops.mineopsapi.shared.infrastructure.persistence.jpa.PageCriteriaTranslator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ShiftQueryServiceImpl implements ShiftQueryService {

    /** Límites que se usan cuando quien consulta pide un rango abierto. */
    private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);
    private static final LocalDate LATEST = LocalDate.of(2100, 1, 1);

    private final ShiftRepository shiftRepository;

    public ShiftQueryServiceImpl(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    @Override
    public PagedResult<Shift> handle(GetAllShiftsQuery query, PageCriteria criteria) {
        var page = findPage(query, PageCriteriaTranslator.toPageable(criteria));
        return PageCriteriaTranslator.toPagedResult(page, criteria);
    }

    @Override
    public List<Shift> handle(GetAllShiftsQuery query) {
        return handle(query, PageCriteria.unpaged()).content();
    }

    private Page<Shift> findPage(GetAllShiftsQuery query, Pageable pageable) {
        var hasRange = query.from() != null || query.to() != null;
        var from = query.from() == null ? EARLIEST : query.from();
        var to = query.to() == null ? LATEST : query.to();

        if (hasRange && query.status() != null) {
            return shiftRepository.findByDateRangeAndStatusOrdered(from, to, query.status(), pageable);
        }
        if (hasRange) {
            return shiftRepository.findByDateRangeOrdered(from, to, pageable);
        }
        if (query.status() != null) {
            return shiftRepository.findByStatusOrdered(query.status(), pageable);
        }
        return shiftRepository.findAllOrdered(pageable);
    }

    @Override
    public Optional<Shift> handle(GetShiftByIdQuery query) {
        return shiftRepository.findById(query.shiftId());
    }
}
