package micro.microservicio_producto.entities.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VentaRequestDTO {
    // En el futuro, aquí podrías añadir 'clienteId', 'tipoComprobante', etc.
    private List<ProductoDTO> items;
}