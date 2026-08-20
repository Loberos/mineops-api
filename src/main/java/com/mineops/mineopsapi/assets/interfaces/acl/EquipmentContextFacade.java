package com.mineops.mineopsapi.assets.interfaces.acl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Capa anticorrupción que publica el contexto de activos.
 * <p>
 * Es la única puerta por la que otro contexto puede leer la flota o sumar horas a una máquina.
 * Mantenerla estrecha es lo que garantiza que el horómetro nunca pueda subir sin que se evalúe el
 * umbral de mantenimiento.
 * </p>
 */
public interface EquipmentContextFacade {

    Optional<EquipmentSnapshot> fetchEquipmentById(Long equipmentId);

    Optional<EquipmentTypeSnapshot> fetchEquipmentTypeById(Long equipmentTypeId);

    List<EquipmentSnapshot> fetchEquipmentByIds(List<Long> equipmentIds);

    List<EquipmentSnapshot> fetchAllEquipment();

    /**
     * Suma horas trabajadas a una máquina, lo que puede cruzar su umbral y bloquearla.
     *
     * @param equipmentId la máquina que trabajó
     * @param hours       horas efectivamente trabajadas
     * @return la máquina después de aplicar el uso
     */
    EquipmentSnapshot registerUsage(Long equipmentId, BigDecimal hours);
}
