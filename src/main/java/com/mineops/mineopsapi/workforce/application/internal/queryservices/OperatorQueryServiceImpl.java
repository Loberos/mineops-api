package com.mineops.mineopsapi.workforce.application.internal.queryservices;

import com.mineops.mineopsapi.workforce.domain.model.aggregates.Operator;
import com.mineops.mineopsapi.workforce.domain.model.entities.Certification;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetAllOperatorsQuery;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetExpiringCertificationsQuery;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetOperatorByDocumentNumberQuery;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetOperatorByIdQuery;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetWorkforceSummaryQuery;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.WorkforceSummary;
import com.mineops.mineopsapi.workforce.domain.services.OperatorQueryService;
import com.mineops.mineopsapi.workforce.infrastructure.persistence.jpa.repositories.CertificationRepository;
import com.mineops.mineopsapi.workforce.infrastructure.persistence.jpa.repositories.OperatorRepository;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;
import com.mineops.mineopsapi.shared.infrastructure.persistence.jpa.PageCriteriaTranslator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OperatorQueryServiceImpl implements OperatorQueryService {

    private final OperatorRepository operatorRepository;
    private final CertificationRepository certificationRepository;

    public OperatorQueryServiceImpl(
            OperatorRepository operatorRepository, CertificationRepository certificationRepository) {
        this.operatorRepository = operatorRepository;
        this.certificationRepository = certificationRepository;
    }

    @Override
    public PagedResult<Operator> handle(GetAllOperatorsQuery query, PageCriteria criteria) {
        var pageable = PageCriteriaTranslator.toPageable(criteria);
        var page = query.status() == null
                ? operatorRepository.findAllOrderedByName(pageable)
                : operatorRepository.findByStatusOrderedByName(query.status(), pageable);
        return PageCriteriaTranslator.toPagedResult(page, criteria);
    }

    @Override
    public List<Operator> handle(GetAllOperatorsQuery query) {
        return handle(query, PageCriteria.unpaged()).content();
    }

    @Override
    public Optional<Operator> handle(GetOperatorByIdQuery query) {
        return operatorRepository.findById(query.operatorId());
    }

    @Override
    public Optional<Operator> handle(GetOperatorByDocumentNumberQuery query) {
        var documentNumber = query.documentNumber() == null ? "" : query.documentNumber().trim().toUpperCase();
        return operatorRepository.findByDocumentNumber(documentNumber);
    }

    @Override
    public List<Certification> handle(GetExpiringCertificationsQuery query) {
        return certificationRepository.findExpiringOnOrBefore(LocalDate.now().plusDays(query.withinDays()));
    }

    @Override
    public WorkforceSummary handle(GetWorkforceSummaryQuery query) {
        return new WorkforceSummary(
                operatorRepository.count(),
                operatorRepository.countWithoutValidCertificationOn(LocalDate.now()));
    }
}
