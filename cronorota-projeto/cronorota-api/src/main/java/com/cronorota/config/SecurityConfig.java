package com.cronorota.config;

import com.cronorota.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

/**
 * UC01 (autenticação) + RN13 (controle de acesso por perfil), agora
 * implementados de verdade via JWT. Ordem de checagem dos matchers importa:
 * o Spring Security avalia de cima pra baixo e usa a PRIMEIRA regra que dá
 * match - por isso as rotas mais específicas (ex.: POST /api/gerentes)
 * precisam vir antes de qualquer regra genérica que também bateria nelas.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // API stateless: cada requisição se autentica sozinha via token,
            // nenhuma sessão de servidor é criada ou consultada.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Login sempre aberto - é a única forma de conseguir um token.
                .requestMatchers("/api/auth/**").permitAll()
                // Ver o aviso de bootstrap no AdministradorController: isto
                // fica aberto só em desenvolvimento.
                .requestMatchers("/api/administradores").permitAll()

                // RN13: só o Administrador cadastra gerente e mexe em parâmetros.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/gerentes").hasAuthority("ROLE_ADMINISTRADOR")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/parametros").hasAuthority("ROLE_ADMINISTRADOR")

                // Tudo o mais exige QUALQUER usuário autenticado (qualquer
                // perfil). Uma separação mais fina por perfil em cada rota de
                // motorista/gerente fica como próximo refinamento - hoje o
                // controle mais visível de "quem vê o quê" está no front-end,
                // que só mostra a navegação de cada perfil.
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
