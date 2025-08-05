package micro.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UserDTO {

    private Long id;

    @NotBlank(message = "El nombre de usuario es un campo requerido.")
    @Size(min = 1, max = 50)
    private String username;

    @NotBlank(message = "La contraseña es un campo requerido.")
    @Size(min = 4, max = 100) // Ajusta según tus políticas de contraseña
    private String password;

    @NotEmpty(message = "Los roles son un campo requerido.")
    private Set<String> authorities;
}