package micro.microservicio_producto.entities.DTO;

public class ProveedorDTO {

    private Long id;

    private String nombre;

    private String contacto; // Email, teléfono, etc.

    public ProveedorDTO() {}

    public ProveedorDTO(String nombre, String contacto) {
        this.nombre = nombre;
        this.contacto = contacto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }
}