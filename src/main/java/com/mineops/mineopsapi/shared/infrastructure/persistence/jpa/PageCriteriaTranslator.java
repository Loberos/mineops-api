package com.mineops.mineopsapi.shared.infrastructure.persistence.jpa;

import com.mineops.mineopsapi.shared.domain.model.valueobjects.PageCriteria;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Frontera entre el vocabulario de paginación del dominio y el de Spring Data.
 * <p>
 * Todo el proyecto habla de {@link PageCriteria} y {@link PagedResult}; solo las implementaciones de
 * los servicios de consulta, que ya dependen de un repositorio, cruzan hasta {@code Pageable} y
 * {@code Page}. Concentrar la traducción en un sitio evita que cada implementación repita la
 * conversión y se desvíe en algún borde.
 * </p>
 */
public final class PageCriteriaTranslator {

    private PageCriteriaTranslator() {
    }

    /**
     * Traduce el criterio al {@code Pageable} que espera el repositorio.
     * <p>
     * Un criterio sin paginar se convierte en {@link Pageable#unpaged()}, que Spring Data resuelve
     * como una consulta sin {@code LIMIT}. Así el mismo método de repositorio sirve para el listado
     * paginado y para los usos internos que necesitan la colección entera.
     * </p>
     */
    public static Pageable toPageable(PageCriteria criteria) {
        return criteria.isPaged() ? PageRequest.of(criteria.page(), criteria.size()) : Pageable.unpaged();
    }

    /**
     * Igual que {@link #toPageable(PageCriteria)}, pero fijando el orden.
     * <p>
     * Lo necesitan las consultas que no traen un {@code order by} propio. Paginar sin orden definido
     * deja a la base en libertad de devolver las filas como le convenga, y dos peticiones seguidas
     * pueden entonces repetir un elemento en una página y saltárselo en la siguiente.
     * </p>
     */
    public static Pageable toPageable(PageCriteria criteria, Sort sort) {
        return criteria.isPaged() ? PageRequest.of(criteria.page(), criteria.size(), sort) : Pageable.unpaged(sort);
    }

    /**
     * Traduce la página devuelta por el repositorio al resultado que expone el dominio.
     * <p>
     * Una página sin paginar informa su propio tamaño como el total de elementos, en vez del
     * {@code Integer.MAX_VALUE} con el que Spring Data representa un tramo ilimitado, que como
     * tamaño de página no le sirve a nadie.
     * </p>
     */
    public static <T> PagedResult<T> toPagedResult(Page<T> page, PageCriteria criteria) {
        var size = criteria.isPaged() ? criteria.size() : (int) page.getTotalElements();
        return new PagedResult<>(page.getContent(), criteria.page(), size, page.getTotalElements());
    }
}
