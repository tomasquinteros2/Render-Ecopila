package micro.eurekaservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/eureka/**") // Deshabilitar CSRF para los endpoints API de Eureka
                )
                .authorizeHttpRequests(authz -> authz
                                .requestMatchers("/eureka/**").permitAll()   // Permitir todas las llamadas API de Eureka
                                .requestMatchers("/").permitAll()            // Permitir acceso al dashboard de Eureka UI en la raíz
                                .requestMatchers("/actuator/**").permitAll() // Permitir acceso a los endpoints de Actuator (ej. health)
                                .anyRequest().denyAll() // Opcional: Denegar cualquier otra petición no especificada.
                );
        return http.build();
    }
}
    