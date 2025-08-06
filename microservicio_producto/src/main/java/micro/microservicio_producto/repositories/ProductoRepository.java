package micro.microservicio_producto.repositories;

import micro.microservicio_producto.entities.Producto;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> , JpaSpecificationExecutor<Producto> {

    // --- MÉTODOS DE BÚSQUEDA CON CARGA EAGER DE RELACIONES ---

    /**
     * Sobrescribe el método base para asegurar la carga de relaciones.
     */
    @Override
    @EntityGraph(attributePaths = {"productosRelacionados"})
    Optional<Producto> findById(Long id);

    /**
     * Sobrescribe el método base para asegurar la carga de relaciones.
     */
    @Override
    @EntityGraph(attributePaths = {"productosRelacionados"})
    List<Producto> findAll();

    /**
     * Sobrescribe el método base para asegurar la carga de relaciones al usar especificaciones.
     */
    @Override
    @EntityGraph(attributePaths = {"productosRelacionados"})
    List<Producto> findAll(Specification<Producto> spec);

    /**
     * Busca productos por descripción, cargando sus relaciones.
     */
    @Query("SELECT p FROM Producto p WHERE LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :desc, '%'))")
    @EntityGraph(attributePaths = {"productosRelacionados"})
    Optional<List<Producto>> findByDesc(@Param("desc") String desc);

    /**
     * Busca un producto por su código único, cargando sus relaciones.
     */
    @Query("SELECT p FROM Producto p WHERE p.codigo_producto = :codigoProducto")
    @EntityGraph(attributePaths = {"productosRelacionados"})
    Optional<Producto> findByCodigo_producto(@Param("codigoProducto") String codigoProducto);

    /**
     * Busca productos por el ID de su tipo, cargando sus relaciones.
     */
    @EntityGraph(attributePaths = {"productosRelacionados"})
    Optional<List<Producto>> findByTipoProductoId(Long id);

    @Query("SELECT p FROM Producto p WHERE p.codigo_producto IN :codigos")
    List<Producto> findAllByCodigo_productoIn(@Param("codigos") List<String> codigos);

    // --- MÉTODOS DE MODIFICACIÓN (NO REQUIEREN @EntityGraph) ---

    @Modifying
    @Query(value = "DELETE FROM \"productos_relacionados\" WHERE \"producto_id\" = :productoId OR \"producto_relacionado_id\" = :productoId", nativeQuery = true)
    void eliminarRelaciones(@Param("productoId") Long productoId);

    @Modifying
    @Query(value = "UPDATE Producto p SET p.cantidad = p.cantidad - :cantidad WHERE p.id = :id AND p.cantidad >= :cantidad")
    int descontar(@Param("id") Long id, @Param("cantidad") Integer cantidad);
}