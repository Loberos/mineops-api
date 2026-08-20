package com.mineops.mineopsapi.workforce.application.internal.commandservices;

import com.mineops.mineopsapi.assets.interfaces.acl.EquipmentContextFacade;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceConflictException;
import com.mineops.mineopsapi.shared.domain.exceptions.ResourceNotFoundException;
import com.mineops.mineopsapi.workforce.domain.model.aggregates.Operator;
import com.mineops.mineopsapi.workforce.domain.model.commands.ChangeOperatorStatusCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.CreateOperatorCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.GrantCertificationCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.RevokeCertificationCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.UpdateOperatorCommand;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.OperatorStatus;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.PersonName;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.ValidityPeriod;
import com.mineops.mineopsapi.workforce.domain.services.OperatorCommandService;
import com.mineops.mineopsapi.workforce.infrastructure.persistence.jpa.repositories.OperatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OperatorCommandServiceImpl implements OperatorCommandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorCommandServiceImpl.class);

    private final OperatorRepository operatorRepository;

    /**
     * Los tipos de equipo pertenecen al contexto de activos, así que la familia que nombra una
     * certificación se valida a través de su fachada publicada y no accediendo a sus tablas.
     */
    private final EquipmentContextFacade equipmentContextFacade;

    public OperatorCommandServiceImpl(
            OperatorRepository operatorRepository, EquipmentContextFacade equipmentContextFacade) {
        this.operatorRepository = operatorRepository;
        this.equipmentContextFacade = equipmentContextFacade;
    }

    @Override
    @Transactional
    public Optional<Operator> handle(CreateOperatorCommand command) {
        var documentNumber = command.documentNumber() == null ? "" : command.documentNumber().trim().toUpperCase();
        if (operatorRepository.existsByDocumentNumber(documentNumber)) {
            throw new ResourceConflictException(
                    "Ya existe un operador con el documento %s".formatted(documentNumber));
        }
        var operator = new Operator(
                command.documentNumber(), new PersonName(command.firstName(), command.lastName()));
        LOGGER.info("Agregando al operador {}", documentNumber);
        return Optional.of(operatorRepository.save(operator));
    }

    @Override
    @Transactional
    public Optional<Operator> handle(UpdateOperatorCommand command) {
        var operator = findOperator(command.operatorId());
        operator.rename(new PersonName(command.firstName(), command.lastName()));
        return Optional.of(operatorRepository.save(operator));
    }

    @Override
    @Transactional
    public Optional<Operator> handle(ChangeOperatorStatusCommand command) {
        var operator = findOperator(command.operatorId());
        if (command.targetStatus() == OperatorStatus.ACTIVE) {
            operator.activate();
        } else {
            operator.deactivate();
        }
        LOGGER.info("El operador {} pasó a {}", operator.getDocumentNumber(), operator.getStatus());
        return Optional.of(operatorRepository.save(operator));
    }

    @Override
    @Transactional
    public Optional<Operator> handle(GrantCertificationCommand command) {
        var operator = findOperator(command.operatorId());
        var equipmentType = equipmentContextFacade.fetchEquipmentTypeById(command.equipmentTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("El tipo de equipo", command.equipmentTypeId()));
        var validity = new ValidityPeriod(command.issuedOn(), command.expiresOn());
        operator.certifyFor(equipmentType.id(), equipmentType.code(), equipmentType.name(), validity);
        LOGGER.info(
                "El operador {} quedó certificado para {} hasta el {}",
                operator.getDocumentNumber(),
                equipmentType.code(),
                command.expiresOn());
        return Optional.of(operatorRepository.save(operator));
    }

    @Override
    @Transactional
    public Optional<Operator> handle(RevokeCertificationCommand command) {
        var operator = findOperator(command.operatorId());
        if (!operator.revokeCertificationFor(command.equipmentTypeId())) {
            throw new ResourceNotFoundException(
                    "El operador %s no tiene certificación para el tipo de equipo %s"
                            .formatted(operator.getDocumentNumber(), command.equipmentTypeId()));
        }
        return Optional.of(operatorRepository.save(operator));
    }

    private Operator findOperator(Long operatorId) {
        return operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("El operador", operatorId));
    }
}
