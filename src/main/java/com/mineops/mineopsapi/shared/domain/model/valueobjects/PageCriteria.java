package com.mineops.mineopsapi.shared.domain.model.valueobjects;

/**
 * Tramo de una lista que pide quien consulta: qué página y de qué tamaño.
 * <p>
 * Se expresa aquí, en el dominio, y no como el {@code Pageable} de Spring Data, para que las
 * interfaces de los servicios de consulta no arrastren la infraestructura de persistencia hasta la
 * capa que las declara. La traducción ocurre en la implementación, que es la única que ya conoce
 * al repositorio.
 * </p>
 * <p>
 * El constructor normaliza en vez de rechazar: una página negativa o un tamaño de cero llegan desde
 * un parámetro de consulta escrito a mano, y para esos casos vale más devolver la primera página
 * que un error. El tope de {@value #MAX_SIZE} evita que {@code ?size=100000} convierta un listado en
 * una descarga completa de la tabla.
 * </p>
 *
 * @param page índice de la página, empezando en cero
 * @param size cantidad de elementos por página, o {@value #UNPAGED_SIZE} si se piden todos
 */
public record PageCriteria(int page, int size) {

    /** Tamaño que se aplica cuando quien consulta no pide uno. */
    public static final int DEFAULT_SIZE = 20;

    /** Techo por página. Acota el costo de una sola petición contra la base y la red. */
    public static final int MAX_SIZE = 100;

    private static final int UNPAGED_SIZE = -1;

    public PageCriteria {
        if (size != UNPAGED_SIZE) {
            page = Math.max(0, page);
            size = Math.min(Math.max(1, size), MAX_SIZE);
        }
    }

    /**
     * Arma el criterio a partir de dos parámetros opcionales de la petición.
     *
     * @param page índice pedido, o null para la primera página
     * @param size tamaño pedido, o null para {@value #DEFAULT_SIZE}
     */
    public static PageCriteria of(Integer page, Integer size) {
        return new PageCriteria(page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

    /**
     * Pide la lista entera, sin trocear.
     * <p>
     * Lo usan los usos internos —los facades entre contextos y el escenario de demostración—, que
     * necesitan la colección completa para recorrerla y nunca la exponen por HTTP. Tenerlo aquí
     * permite que exista un solo camino de consulta en vez de uno paginado y otro sin paginar.
     * </p>
     */
    public static PageCriteria unpaged() {
        return new PageCriteria(0, UNPAGED_SIZE);
    }

    /** Si el criterio acota a una página, en vez de pedir la colección entera. */
    public boolean isPaged() {
        return size != UNPAGED_SIZE;
    }
}
