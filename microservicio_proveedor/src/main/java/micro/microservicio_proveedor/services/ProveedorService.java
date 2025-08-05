package micro.microservicio_proveedor.services;

import com.fasterxml.jackson.databind.ObjectMapper;



import jakarta.transaction.Transactional;
import micro.microservicio_proveedor.entities.Proveedor;
import micro.microservicio_proveedor.exceptions.BusinessLogicException;
import micro.microservicio_proveedor.exceptions.ResourceNotFoundException;
import micro.microservicio_proveedor.repositories.ProveedorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProveedorService {

    private static final Logger log = LoggerFactory.getLogger(ProveedorService.class); // Inicializar Logger

    private final ProveedorRepository proveedorRepository;

    private final ObjectMapper objectMapper;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
        this.objectMapper = new ObjectMapper();
    }
    @Cacheable("proveedores")
    public List<Proveedor> findAll() {
        log.info("Buscando todos los proveedores.");
        return proveedorRepository.findAll();
    }
    @Cacheable(value = "proveedor", key = "#id")
    public Proveedor findById(Long id) {
        log.debug("Buscando proveedor con ID: {}", id);
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con ID: " + id));
    }

    @CacheEvict(value = "proveedores", allEntries = true)
    @Transactional
    public Proveedor save(Proveedor proveedor) {
        Proveedor proveedorGuardado = proveedorRepository.save(proveedor);
        return proveedorGuardado;
    }

    @Caching(
            put = { @CachePut(value = "proveedor", key = "#id") },
            evict = { @CacheEvict(value = "proveedores", allEntries = true) }
    )
    @Transactional
    public Proveedor update(Long id, Proveedor proveedorDetails) {
        log.debug("Actualizando proveedor con ID: {}", id);
        Proveedor existente = findById(id);

        if (proveedorDetails.getNombre() != null && !proveedorDetails.getNombre().trim().isEmpty()) {
            Optional<Proveedor> existingWithName = proveedorRepository.findByNombre(proveedorDetails.getNombre());
            if (existingWithName.isPresent() && !existingWithName.get().getId().equals(id)) {
                throw new BusinessLogicException("Ya existe otro proveedor con el nombre: " + proveedorDetails.getNombre());
            }
            existente.setNombre(proveedorDetails.getNombre());
        }
        if (proveedorDetails.getContacto() != null && !proveedorDetails.getContacto().trim().isEmpty()) {
            existente.setContacto(proveedorDetails.getContacto());
        }

        Proveedor proveedorActualizado = proveedorRepository.save(existente);

        return proveedorActualizado;
    }

    @Caching(evict = {
            @CacheEvict(value = "proveedor", key = "#id"),
            @CacheEvict(value = "proveedores", allEntries = true)
    })
    @Transactional
    public void delete(Long id) {
        log.debug("Eliminando proveedor con ID: {}", id);
        if (!proveedorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Proveedor no encontrado con ID: " + id);
        }
        proveedorRepository.deleteById(id);
        log.info("Proveedor con ID {} eliminado correctamente.", id);
    }
}