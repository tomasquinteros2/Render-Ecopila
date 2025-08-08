package micro.microservicio_producto.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import jakarta.persistence.criteria.Predicate;
import micro.microservicio_producto.entities.DTO.*;
import micro.microservicio_producto.entities.Producto;
import micro.microservicio_producto.exceptions.BusinessLogicException;
import micro.microservicio_producto.exceptions.ResourceNotFoundException;
import micro.microservicio_producto.feignClients.DolarFeignClient;
import micro.microservicio_producto.feignClients.ProveedorClient;
import micro.microservicio_producto.feignClients.TipoProductoClient;
import micro.microservicio_producto.repositories.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);

    private final ProductoRepository productoRepository;
    private final DolarFeignClient dolarFeignClient;
    private final TipoProductoClient tipoProductoClient;
    private final ProveedorClient proveedorClient;
    private final ObjectMapper objectMapper;

    @Value("${app.dolar.default-value:1200.00}")
    private String dolarDefaultValue;

    public ProductoService(ProductoRepository productoRepository,
                           DolarFeignClient dolarFeignClient,
                           TipoProductoClient tipoProductoClient,
                           ProveedorClient proveedorClient) {
        this.productoRepository = productoRepository;
        this.dolarFeignClient = dolarFeignClient;
        this.tipoProductoClient = tipoProductoClient;
        this.proveedorClient = proveedorClient;
        this.objectMapper = new ObjectMapper();
    }

    // --- MÉTODOS DE LECTURA ---
    @Transactional
    public List<Producto> findAllFiltered(String search, Long proveedorId, Long tipoId) {
        Specification<Producto> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.trim().isEmpty()) {
                String likePattern = "%" + search.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("codigo_producto")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("descripcion")), likePattern)
                ));
            }
            if (proveedorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("proveedorId"), proveedorId));
            }
            if (tipoId != null) {
                predicates.add(criteriaBuilder.equal(root.get("tipoProductoId"), tipoId));
            }
            query.orderBy(criteriaBuilder.desc(root.get("id")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        List<Producto> productos = productoRepository.findAll(spec);
        return productos;
    }

    public List<Producto> findAll() {
        return productoRepository.findAll();
    }

    public Producto findById(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    public List<Producto> findByDesc(String desc) {
        return productoRepository.findByDesc(desc)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontraron productos con la descripción: " + desc));
    }

    public List<Producto> findByTipoProducto(Long id) {
        return productoRepository.findByTipoProductoId(id).orElse(List.of());
    }

    // --- MÉTODOS DE ESCRITURA Y LÓGICA DE NEGOCIO ---

    @Transactional
    public Producto save(Producto incomingProduct) {
        // Usamos findBy para que el Optional sea más explícito
        Optional<Producto> existingProductOpt = productoRepository.findByCodigo_producto(incomingProduct.getCodigo_producto());

        // Determina si estamos actualizando un producto existente o creando uno nuevo.
        Producto productToSave = existingProductOpt.orElse(incomingProduct);

        if (existingProductOpt.isPresent()) {
            log.info("Producto con código {} ya existe. Actualizando datos y stock.", incomingProduct.getCodigo_producto());

            // 1. Actualiza los campos desde el producto entrante
            productToSave.setDescripcion(incomingProduct.getDescripcion());
            productToSave.setPrecio_sin_iva(incomingProduct.getPrecio_sin_iva());
            productToSave.setPorcentaje_ganancia(incomingProduct.getPorcentaje_ganancia());
            productToSave.setIva(incomingProduct.getIva());
            // ... actualiza cualquier otro campo que pueda cambiar

            // 2. Maneja la lógica de la cantidad
            productToSave.setCantidad(productToSave.getCantidad() + incomingProduct.getCantidad());

        } else {
            log.info("Creando nuevo producto con código {}", incomingProduct.getCodigo_producto());
            // Asegura que la cantidad sea al menos 1 para productos nuevos
            productToSave.setCantidad(incomingProduct.getCantidad() > 0 ? incomingProduct.getCantidad() : 1);
        }

        // 3. Lógica común para ambos casos (creación y actualización)
        productToSave.setFecha_ingreso(LocalDate.now());

        // Valida las relaciones (proveedor, tipo de producto)
        validarRelaciones(productToSave.getTipoProductoId(), productToSave.getProveedorId());

        // 4. Recalcula siempre los precios para reflejar cualquier cambio
        BigDecimal valorDolar = obtenerValorDolar();
        recalculatePrices(productToSave, valorDolar);

        // 5. Guarda y devuelve la entidad persistida
        return productoRepository.save(productToSave);
    }

    @Transactional
    public Producto update(Long id, Producto productoDetails) {
        Producto productoExistente = findById(id);
        log.info("Actualizando producto ID: {}", id);
        updateProductoFields(productoExistente, productoDetails);
        BigDecimal valorDolar = obtenerValorDolar();
        recalculatePrices(productoExistente, valorDolar);
        Producto productoActualizado = productoRepository.save(productoExistente);
        return productoActualizado;
    }

    @Transactional
    public void delete(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new ResourceNotFoundException("No se puede eliminar. Producto no encontrado con ID: " + id);
        }
        productoRepository.eliminarRelaciones(id);
        productoRepository.deleteById(id);
    }

    @Transactional
    public void descontarProductos(List<ProductoDTO> productos) {
        if (productos == null || productos.isEmpty()) {
            throw new IllegalArgumentException("La lista de productos a descontar no puede estar vacía.");
        }
        for (ProductoDTO productoDTO : productos) {
            int updatedRows = productoRepository.descontar(productoDTO.getId(), productoDTO.getCantidad());
            if (updatedRows == 0) {
                throw new BusinessLogicException("No se pudo descontar producto con ID " + productoDTO.getId() + " (cantidad insuficiente o no existe)");
            }
        }
    }

    @Transactional
    public void agregarRelacion(ProductoRelacionadoDTO dto) {
        Producto producto1 = findById(dto.getProductoId());
        Producto producto2 = findById(dto.getProductoRelacionadoId());
        if (!Objects.equals(producto1.getTipoProductoId(), producto2.getTipoProductoId())) {
            throw new BusinessLogicException("Los productos deben ser del mismo tipo para poder relacionarlos.");
        }
        producto1.agregarRelacion(producto2);
        Producto productoGuardado = productoRepository.save(producto1);
    }
    @Transactional
    public void eliminarRelacion(ProductoRelacionadoDTO dto) {
        Producto productoPrincipal = findById(dto.getProductoId());
        Producto productoARemover = findById(dto.getProductoRelacionadoId());

        // La lógica en la entidad Producto se encarga de la bidireccionalidad
        productoPrincipal.eliminarRelacion(productoARemover);

        // No es necesario un .save() explícito, ya que la entidad está "managed"
        // por JPA dentro de la transacción. Los cambios se guardarán al finalizar.
        log.info("Relación entre producto ID {} y producto ID {} eliminada.", dto.getProductoId(), dto.getProductoRelacionadoId());
    }

    // CAMBIO: Añadimos @Transactional para evitar LazyInitializationException al acceder a getProductosRelacionados()
    @Transactional(readOnly = true)
    public List<ProductoRelacionadoResultadoDTO> obtenerRelacionadosConProveedor(Long productoId) {
        Producto producto = findById(productoId);
        Set<Producto> relacionados = producto.getProductosRelacionados();
        List<ProductoRelacionadoResultadoDTO> dtos = new ArrayList<>();
        for (Producto p : relacionados) {
            try {
                ProveedorDTO proveedor = proveedorClient.getProveedorById(p.getProveedorId()).getBody();
                TipoProductoDTO tipo = tipoProductoClient.getTipoProductoById(p.getTipoProductoId()).getBody();
                dtos.add(new ProductoRelacionadoResultadoDTO(p.getId(), p.getDescripcion(), proveedor.getNombre(), p.getPrecio_publico(), tipo.getNombre()));
            } catch (FeignException e) {
                log.error("No se pudo obtener información completa para el producto relacionado ID: {}. Causa: {}", p.getId(), e.getMessage());
            }
        }
        return dtos;
    }

    // --- MÉTODOS DE SINCRONIZACIÓN (ONLINE/NUBE) ---

    @Transactional
    @Profile("online")
    @Scheduled(cron = "0 */5 * * * *")
    public void actualizarPreciosProgramado() {
        log.info("--- Iniciando tarea programada: Actualización de precios ---");
        BigDecimal valorDolar = obtenerValorDolar();
        long productosActualizados = 0;
        try (Stream<Producto> productoStream = productoRepository.findAllAsStream()) {
            productoStream.forEach(producto -> {
                recalculatePrices(producto, valorDolar);
            });
            productosActualizados = productoRepository.count();
        }
        log.info("--- Finalizada tarea programada: {} productos revisados y actualizados con valor de dólar {} ---", productosActualizados, valorDolar);
    }


    private void recalculatePrices(Producto producto, BigDecimal valorDolar) {
        if (producto.getPrecio_sin_iva() == null || producto.getIva() == null || producto.getPorcentaje_ganancia() == null) {
            log.warn("Producto ID {} no tiene los datos base (precio sin iva, iva, ganancia) para calcular precios. Saltando...", producto.getId());
            return;
        }
        BigDecimal cien = new BigDecimal("100");
        BigDecimal porcentajeGanancia = producto.getPorcentaje_ganancia().divide(cien, 4, RoundingMode.HALF_UP);
        BigDecimal costoDolares = producto.getPrecio_sin_iva().multiply(BigDecimal.ONE.add(producto.getIva()));
        producto.setCosto_dolares(costoDolares.setScale(4, RoundingMode.HALF_UP));
        BigDecimal costoPesos = costoDolares.multiply(valorDolar);
        producto.setCosto_pesos(costoPesos.setScale(4, RoundingMode.HALF_UP));
        BigDecimal precioPublicoUs = costoDolares.multiply(BigDecimal.ONE.add(porcentajeGanancia));
        producto.setPrecio_publico_us(precioPublicoUs.setScale(4, RoundingMode.HALF_UP));
        BigDecimal precioSinRedondear = precioPublicoUs.multiply(valorDolar);
        producto.setPrecio_sin_redondear(precioSinRedondear.setScale(4, RoundingMode.HALF_UP));
        BigDecimal resto = producto.getResto() != null && producto.getResto().compareTo(BigDecimal.ZERO) > 0
                ? producto.getResto()
                : new BigDecimal("1000");
        BigDecimal precioPublico = precioSinRedondear.divide(resto, 0, RoundingMode.CEILING).multiply(resto);
        producto.setPrecio_publico(precioPublico.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal obtenerValorDolar() {
        try {
            return dolarFeignClient.getValorDolar(1).getBody();
        } catch (Exception e) {
            log.error("No se pudo obtener el valor del dólar del microservicio. Usando valor por defecto: {}. Causa: {}", dolarDefaultValue, e.getMessage());
            return new BigDecimal(dolarDefaultValue);
        }
    }

    private void validarRelaciones(Long tipoProductoId, Long proveedorId) {
        try {
            Objects.requireNonNull(tipoProductoId, "El ID del tipo de producto no puede ser nulo.");
            Objects.requireNonNull(proveedorId, "El ID del proveedor no puede ser nulo.");
            tipoProductoClient.getTipoProductoById(tipoProductoId);
            proveedorClient.getProveedorById(proveedorId);
        } catch (NullPointerException ex) {
            throw new BusinessLogicException(ex.getMessage());
        } catch (FeignException.NotFound ex) {
            log.warn("Error de validación de relaciones: {}", ex.getMessage());
            throw new ResourceNotFoundException("El tipo de producto o el proveedor especificado no existe.");
        }
    }

    private void updateProductoFields(Producto target, Producto source) {
        if (source.getCodigo_producto() != null) target.setCodigo_producto(source.getCodigo_producto());
        if (source.getDescripcion() != null) target.setDescripcion(source.getDescripcion());
        if (source.getTipoProductoId() != null) target.setTipoProductoId(source.getTipoProductoId());
        if (source.getProveedorId() != null) target.setProveedorId(source.getProveedorId());
        if (source.getFecha_ingreso() != null) target.setFecha_ingreso(source.getFecha_ingreso());
        if (source.getCantidad() > 0) target.setCantidad(source.getCantidad());
        if (source.getIva() != null) target.setIva(source.getIva());
        if (source.getPorcentaje_ganancia() != null) target.setPorcentaje_ganancia(source.getPorcentaje_ganancia());
        if (source.getPrecio_sin_iva() != null) target.setPrecio_sin_iva(source.getPrecio_sin_iva());
        if (source.getResto() != null) target.setResto(source.getResto());
        if (source.getProductosRelacionadosIds() != null) {
            Set<Producto> nuevasRelaciones = new HashSet<>(productoRepository.findAllById(source.getProductosRelacionadosIds()));
            target.setProductosRelacionados(nuevasRelaciones);
        }
    }

    @Transactional
    public void saveAllProducts(List<Producto> incomingProducts) {
        if (incomingProducts == null || incomingProducts.isEmpty()) {
            return;
        }

        // --- PASO 1: LEER UNA SOLA VEZ ---
        // Extrae todos los códigos de producto de la lista entrante.
        List<String> productCodes = incomingProducts.stream()
                .map(Producto::getCodigo_producto)
                .collect(Collectors.toList());

        // Realiza UNA SOLA consulta a la BD para obtener todos los productos que ya existen.
        List<Producto> existingProductsFromDB = productoRepository.findAllByCodigo_productoIn(productCodes);

        // Crea un mapa para una búsqueda en memoria súper rápida (O(1)).
        Map<String, Producto> existingProductsMap = existingProductsFromDB.stream()
                .collect(Collectors.toMap(Producto::getCodigo_producto, p -> p));

        // --- PASO 2: PROCESAR EN MEMORIA ---
        BigDecimal valorDolar = obtenerValorDolar(); // Obtén el valor del dólar una sola vez.
        List<Producto> productsToPersist = new ArrayList<>();

        for (Producto incomingProduct : incomingProducts) {
            // Busca el producto en nuestro mapa en memoria, no en la BD.
            Producto existingProduct = existingProductsMap.get(incomingProduct.getCodigo_producto());

            if (existingProduct != null) {
                // --- LÓGICA DE ACTUALIZACIÓN ---
                log.info("Producto con código {} ya existe. Actualizando datos y stock.", incomingProduct.getCodigo_producto());

                // Actualiza los campos del producto existente con los datos del nuevo.
                updateProductoFields(existingProduct, incomingProduct);

                // Suma la cantidad al stock existente.
                existingProduct.setCantidad(existingProduct.getCantidad() + incomingProduct.getCantidad());

                existingProduct.setFecha_ingreso(LocalDate.now());
                recalculatePrices(existingProduct, valorDolar);
                productsToPersist.add(existingProduct);

            } else {
                // --- LÓGICA DE CREACIÓN ---
                log.info("Preparando nuevo producto con código {}", incomingProduct.getCodigo_producto());

                // Asegura que la cantidad sea al menos 1.
                incomingProduct.setCantidad(incomingProduct.getCantidad() > 0 ? incomingProduct.getCantidad() : 1);
                incomingProduct.setFecha_ingreso(LocalDate.now());
                recalculatePrices(incomingProduct, valorDolar);
                productsToPersist.add(incomingProduct);
            }
        }

        // --- PASO 3: ESCRIBIR UNA SOLA VEZ ---
        if (!productsToPersist.isEmpty()) {
            // Valida las relaciones de todos los productos antes de guardar.
            // (Esta es una optimización opcional, podrías moverla dentro del bucle si prefieres).
            for (Producto p : productsToPersist) {
                validarRelaciones(p.getTipoProductoId(), p.getProveedorId());
            }

            log.info("Guardando {} productos en la base de datos en una sola operación.", productsToPersist.size());
            productoRepository.saveAll(productsToPersist);
        }
    }
}
