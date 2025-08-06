package micro.microservicio_producto.config;

import micro.microservicio_producto.entities.NroComprobante;
import micro.microservicio_producto.repositories.ComprobanteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final ComprobanteRepository comprobanteRepository;

    public DataInitializer(ComprobanteRepository comprobanteRepository) {
        this.comprobanteRepository = comprobanteRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Comprueba si la tabla de comprobantes está vacía
        if (comprobanteRepository.count() == 0) {
            log.info("La tabla de comprobantes está vacía. Inicializando el primer registro contador...");

            // Crea el registro contador inicial
            NroComprobante contadorInicial = new NroComprobante();
            contadorInicial.setId(1L); // Le asignamos explícitamente el ID 1
            contadorInicial.setPrefijo("AA");
            contadorInicial.setNumero(0); // El primer comprobante será el 1

            comprobanteRepository.save(contadorInicial);
            log.info("Registro contador de comprobantes creado con éxito.");
        } else {
            log.info("El registro contador de comprobantes ya existe. No se requiere inicialización.");
        }
    }
}
    