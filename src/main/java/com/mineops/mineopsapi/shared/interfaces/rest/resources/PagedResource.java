package com.mineops.mineopsapi.shared.interfaces.rest.resources;

import com.mineops.mineopsapi.shared.domain.model.valueobjects.PagedResult;

import java.util.List;

/**
 * Envoltorio único de página que devuelve cada listado paginado de la API.
 * <p>
 * Se declara aquí en vez de serializar el {@code Page} de Spring Data porque ese tipo expone su
 * estructura interna —{@code pageable}, {@code sort}, {@code numberOfElements}— que el propio
 * Spring advierte que puede cambiar entre versiones. Un contrato propio se documenta con precisión
 * en Swagger y sobrevive a una actualización del framework.
 * </p>
 * <p>
 * {@code totalPages}, {@code first} y {@code last} se derivan de los otros campos y podrían
 * calcularse en el cliente. Viajan igual porque son exactamente lo que un paginador necesita para
 * dibujarse, y recalcularlos en cada consumidor es repetir en varios sitios una aritmética con
 * bordes fáciles de equivocar.
 * </p>
 *
 * @param content       elementos de esta página
 * @param page          índice de esta página, empezando en cero
 * @param size          tamaño de página aplicado, que puede ser menor al pedido si excedía el tope
 * @param totalElements cantidad de elementos en la lista completa
 * @param totalPages    cantidad de páginas que ocupa la lista completa; nunca menor que uno
 * @param first         si esta es la primera página
 * @param last          si después de esta no hay más páginas
 * @param <T>           tipo del recurso que se lista
 */
public record PagedResource<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PagedResource<T> fromResult(PagedResult<T> result) {
        return new PagedResource<>(
                result.content(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.first(),
                result.last());
    }
}
