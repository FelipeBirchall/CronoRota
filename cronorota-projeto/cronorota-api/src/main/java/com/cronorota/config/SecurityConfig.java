package com.cronorota.config;

import com.cronorota.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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
            // Sem token (ou token expirado) = 401; token válido mas perfil sem
            // permissão = 403. Sem esta linha o Spring devolve 403 nos dois
            // casos, e o front-end não teria como distinguir "sessão expirou,
            // faça login de novo" de "você não pode ver isto".
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // Login sempre aberto - é a única forma de conseguir um token.
                .requestMatchers("/api/auth/**").permitAll()
                // Ver o aviso de bootstrap no AdministradorController: isto
                // fica aberto só em desenvolvimento.
                .requestMatchers("/api/administradores").permitAll()

                // RN13: só o Administrador cadastra gerente e mexe em parâmetros.
                .requestMatchers(HttpMethod.POST, "/api/gerentes").hasAuthority("ROLE_ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/parametros").hasAuthority("ROLE_ADMINISTRADOR")

                // Operação do gerente (UC02, UC04, UC05, UC06). Montar roteiro
                // e cadastrar motorista usam o id do gerente logado, então só
                // fazem sentido com perfil de gerente.
                .requestMatchers(HttpMethod.POST, "/api/motoristas", "/api/enderecos", "/api/pedidos", "/api/roteiros")
                    .hasAuthority("ROLE_GERENTE")
                .requestMatchers("/api/motoristas/**").hasAnyAuthority("ROLE_GERENTE", "ROLE_ADMINISTRADOR")
                .requestMatchers("/api/pedidos/**", "/api/enderecos/**").hasAuthority("ROLE_GERENTE")
                .requestMatchers(HttpMethod.GET, "/api/roteiros/meus").hasAuthority("ROLE_MOTORISTA")

                // O resto (consultar roteiro, registrar chegada/saída) vale
                // para qualquer perfil autenticado - quem pode ver QUAL
                // roteiro (RN13) é decidido no RoteiroService.verificarAcesso,
                // porque depende do dono do registro, não só do perfil.
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
