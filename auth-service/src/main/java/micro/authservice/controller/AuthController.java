package micro.authservice.controller;

import jakarta.validation.Valid;
import micro.authservice.dto.JwtTokenDTO;
import micro.authservice.dto.LoginDTO;
import micro.authservice.dto.UserDTO;
import micro.authservice.entity.Usuario;
import micro.authservice.security.jwt.TokenProvider;
import micro.authservice.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final TokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    public AuthController(TokenProvider tokenProvider, AuthenticationManager authenticationManager, UserService userService) {
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtTokenDTO> authorize(@Valid @RequestBody LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Aquí podrías añadir lógica para "rememberMe" si lo deseas
        String jwt = tokenProvider.createToken(authentication, false);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwt); // Añadir el token al header

        return new ResponseEntity<>(new JwtTokenDTO(jwt), httpHeaders, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerAccount(@Valid @RequestBody UserDTO userDTO) {
        try {
            Usuario newUser = userService.registerUser(userDTO);
            // Convertir Usuario a UserDTO para la respuesta (sin la contraseña)
            UserDTO responseDto = new UserDTO();
            responseDto.setId(newUser.getId());
            responseDto.setUsername(newUser.getUsername());
            responseDto.setAuthorities(userDTO.getAuthorities()); // O leerlos de newUser.getAuthorities()
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (UserService.UsernameAlreadyUsedException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("X-error", "usernameAlreadyUsed").build();
        } catch (UserService.AuthorityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("X-error", "authorityNotFound").build();
        }
    }
}