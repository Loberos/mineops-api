package com.mineops.mineopsapi.shared.domain.model.valueobjects;

import java.util.List;
import java.util.function.Function;

/**
 * Una página ya resuelta: los elementos del tramo pedido y cuántos hay en total.
 * <p>
 * Es el reverso de {@link PageCriteria} y existe por el mismo motivo: que la firma de un servicio de
 * consulta no mencione el {@code Page} de Spring Data. El total viaja junto a los elementos porque
 * sin él quien consulta no puede saber cuántas páginas hay, y averiguarlo le costaría una segunda
 * petición.
 * </p>
 *
 * @param content       elementos de esta página, en el orden que fijó la consulta
 * @param page          índice de esta página, empezando en cero
 * @param size          tamaño de página aplicado
 * @param totalElements cantidad de elementos que hay en la lista completa, no solo en esta página
 * @param <T>           tipo de los elementos
 */
public record PagedResult<T>(List<T> content, int page, int size, long totalElements) {

    /** Página única que contiene una lista entera. Para las consultas que no se trocean. */
    public static <T> PagedResult<T> of(List<T> content) {
        return new PagedResult<>(content, 0, content.size(), content.size());
    }

    /**
     * Convierte los elementos conservando los datos del tramo.
     * <p>
     * Permite que la capa REST pase de agregados a recursos de transporte sin rearmar a mano el
     * total ni los índices, que es justo donde se cuelan los errores de paginación.
     * </p>
     */
    public <R> PagedResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PagedResult<>(content.stream().<R>map(mapper).toList(), page, size, totalElements);
    }

    /**
     * Cantidad de páginas que ocupa la lista completa.
     * <p>
     * Una lista vacía ocupa una página, no cero: quien consulta pidió una página y recibió una,
     * aunque venga sin elementos.
     * </p>
     */
    public int totalPages() {
        if (size <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil((double) totalElements / size));
    }

    /** Si esta es la primera página. */
    public boolean first() {
        return page <= 0;
    }

    /** Si después de esta no hay más páginas. */
    public boolean last() {
        return page >= totalPages() - 1;
    }
}
