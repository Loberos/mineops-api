package com.mineops.mineopsapi.assets.domain.model.queries;

import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;

/**
 * Lista la flota, con filtros opcionales.
 *
 * @param status          deja solo las máquinas en este estado; null deja todos los estados
 * @param equipmentTypeId deja solo las máquinas de esta familia; null deja todas las familias
 */
public record GetAllEquipmentQuery(EquipmentStatus status, Long equipmentTypeId) {

    public static GetAllEquipmentQuery unfiltered() {
        return new GetAllEquipmentQuery(null, null);
    }
}
