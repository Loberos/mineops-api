package com.mineops.mineopsapi.workforce.interfaces.acl;

import java.util.List;
import java.util.Optional;

/**
 * Capa anticorrupción que publica el contexto de personal. Es de solo lectura: ningún otro contexto
 * tiene motivo para cambiar quién está certificado para qué.
 */
public interface OperatorContextFacade {

    Optional<OperatorSnapshot> fetchOperatorById(Long operatorId);

    List<OperatorSnapshot> fetchOperatorsByIds(List<Long> operatorIds);

    List<OperatorSnapshot> fetchAllOperators();
}
