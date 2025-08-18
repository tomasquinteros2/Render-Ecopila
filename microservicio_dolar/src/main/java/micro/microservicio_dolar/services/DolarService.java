package micro.microservicio_dolar.services;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import micro.microservicio_dolar.entities.dto.DolarApiResponseDTO;
import micro.microservicio_dolar.entities.Dolar;
import micro.microservicio_dolar.repository.DolarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class DolarService {

    private static final Logger log = LoggerFactory.getLogger(DolarService.class);

    private final DolarRepository dolarRepository;
    private final RestTemplate restTemplate;

    @Value("${app.dolar.default-price:1300.00}")
    private String defaultPriceStr;

    @Value("${app.offline-mode:false}")
    private boolean offlineMode;

    @Value("${app.dolar.update-enabled:true}")
    private boolean updateEnabled;

    public DolarService(DolarRepository dolarRepository, RestTemplate restTemplate) {
        this.dolarRepository = dolarRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public void init() {
        if (dolarRepository.count() == 0) {
            log.info("No hay registro de Dólar en la base de datos. Creando uno inicial...");
            BigDecimal initialPrice;
            String initialName;

            if (offlineMode) {
                // MODO OFFLINE: Arranca con el valor por defecto. Esperará la sincronización para el valor real.
                initialPrice = new BigDecimal(defaultPriceStr);
                initialName = "Dólar Oficial (Offline)";
                log.info("Modo OFFLINE: Precio inicial establecido por defecto: {}. Esperando sincronización.", initialPrice);
            } else {
                // MODO ONLINE: Intenta obtener el precio real de la API al arrancar.
                initialPrice = obtenerPrecioDolarDeApi()
                        .orElse(new BigDecimal(defaultPriceStr));
                initialName = "Dólar Oficial";
                log.info("Modo ONLINE: Precio inicial obtenido de la API (o por defecto si falla): {}", initialPrice);
            }

            Dolar dolar = new Dolar();
            dolar.setNombre(initialName);
            dolar.setPrecio(initialPrice);
            dolarRepository.save(dolar);
        }
    }

    // --- MÉTODOS CRUD ---
    @Cacheable("dolares")
    @Transactional
    public List<Dolar> findAll() {
        return dolarRepository.findAll();
    }

    @Cacheable(value = "dolar", key = "#id")
    @Transactional
    public Dolar findById(Long id) {
        return dolarRepository.findById(id).orElse(null);
    }

    @Cacheable(value = "dolar", key = "#id")
    @Transactional
    public BigDecimal getValorDolar(Long id) {
        return dolarRepository.findById(id)
                .map(Dolar::getPrecio)
                .orElseThrow(() -> new NoSuchElementException("No se encontró el valor del dólar con ID: " + id));
    }

    @CacheEvict(value = "dolares", allEntries = true)
    @Transactional
    public Dolar save(Dolar dolar) {
        Dolar newDolar = dolarRepository.save(dolar);
        log.info("Dólar guardado: {}", newDolar);
        return newDolar;
    }

    @Caching(
            put = { @CachePut(value = "dolar", key = "#id") },
            evict = { @CacheEvict(value = "dolares", allEntries = true) }
    )
    @Transactional
    public Dolar update(Long id, Dolar dolarDetails) {
        Dolar existingDolar = dolarRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Dólar no encontrado con ID: " + id));

        existingDolar.setNombre(dolarDetails.getNombre());
        existingDolar.setPrecio(dolarDetails.getPrecio());

        Dolar updatedDolar = dolarRepository.save(existingDolar);
        log.info("Dólar actualizado ID {}: {}", id, updatedDolar);
        return updatedDolar;
    }

    @Caching(evict = {
            @CacheEvict(value = "dolar", key = "#id"),
            @CacheEvict(value = "dolares", allEntries = true)
    })
    @Transactional
    public void delete(Long id) {
        if (!dolarRepository.existsById(id)) {
            throw new NoSuchElementException("El dólar con ID " + id + " no existe y no puede ser eliminado.");
        }
        dolarRepository.deleteById(id);
        log.info("Dólar eliminado con ID: {}", id);
    }


    // --- LÓGICA DE ACTUALIZACIÓN ---

    @Transactional
    public Optional<BigDecimal> obtenerPrecioDolarDeApi() {
        String url = "https://dolarapi.com/v1/dolares";
        try {
            ResponseEntity<List<DolarApiResponseDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<DolarApiResponseDTO>>() {}
            );

            List<DolarApiResponseDTO> dolares = response.getBody();
            if (dolares == null) {
                log.warn("La respuesta de la API de dólar fue nula.");
                return Optional.empty();
            }

            return dolares.stream()
                    .filter(d -> "oficial".equalsIgnoreCase(d.getCasa()))
                    .map(DolarApiResponseDTO::getVenta)
                    .findFirst();

        } catch (RestClientException e) {
            log.error("Error obteniendo el valor del dólar de la API externa: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Scheduled(cron = "0 30 * * * *")
    public void actualizarPrecioDolar() {
        if (!isOnline()) {
            return;
        }

        log.info("MODO ONLINE: Iniciando tarea programada de actualización de precio del dólar.");

        //  Se actualiza el precio SOLO si se obtiene un nuevo valor de la API.
        obtenerPrecioDolarDeApi().ifPresent(nuevoPrecio -> {
            log.info("Nuevo precio de dólar obtenido de la API: {}", nuevoPrecio);

            dolarRepository.findAll().stream().findFirst().ifPresentOrElse(
                    dolar -> {
                        dolar.setPrecio(nuevoPrecio);
                        this.update(dolar.getId(), dolar); // Usamos update para invalidar caché
                        log.info("Precio del dólar en la BD (master) actualizado a: {}. El cambio se replicará a los clientes.", nuevoPrecio);
                    },
                    () -> log.error("Se obtuvo un precio de la API, pero no se encontró ningún registro de Dólar en la BD para actualizar.")
            );
        });
    }

    private boolean isOnline() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
            return true;
        } catch (IOException e) {
            log.warn("No hay conexión a internet. La actualización del dólar se omitirá.");
            return false;
        }
    }
}