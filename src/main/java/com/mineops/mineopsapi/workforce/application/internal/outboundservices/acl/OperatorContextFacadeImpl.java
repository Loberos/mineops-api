package com.mineops.mineopsapi.workforce.application.internal.outboundservices.acl;

import com.mineops.mineopsapi.workforce.domain.model.queries.GetAllOperatorsQuery;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetOperatorByIdQuery;
import com.mineops.mineopsapi.workforce.domain.services.OperatorQueryService;
import com.mineops.mineopsapi.workforce.infrastructure.persistence.jpa.repositories.OperatorRepository;
import com.mineops.mineopsapi.workforce.interfaces.acl.OperatorContextFacade;
import com.mineops.mineopsapi.workforce.interfaces.acl.OperatorSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class OperatorContextFacadeImpl implements OperatorContextFacade {

    private final OperatorQueryService operatorQueryService;
    private final OperatorRepository operatorRepository;

    public OperatorContextFacadeImpl(
            OperatorQueryService operatorQueryService, OperatorRepository operatorRepository) {
        this.operatorQueryService = operatorQueryService;
        this.operatorRepository = operatorRepository;
    }

    @Override
    public Optional<OperatorSnapshot> fetchOperatorById(Long operatorId) {
        return operatorQueryService.handle(new GetOperatorByIdQuery(operatorId))
                .map(OperatorSnapshot::fromAggregate);
    }

    @Override
    public List<OperatorSnapshot> fetchOperatorsByIds(List<Long> operatorIds) {
        if (operatorIds == null || operatorIds.isEmpty()) {
            return List.of();
        }
        return operatorRepository.findAllById(operatorIds).stream()
                .map(OperatorSnapshot::fromAggregate)
                .toList();
    }

    @Override
    public List<OperatorSnapshot> fetchAllOperators() {
        return operatorQueryService.handle(GetAllOperatorsQuery.unfiltered()).stream()
                .map(OperatorSnapshot::fromAggregate)
                .toList();
    }
}
