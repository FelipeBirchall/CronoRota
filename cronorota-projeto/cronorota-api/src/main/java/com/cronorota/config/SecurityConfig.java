package com.cronorota.config;

import com.cronorota.dto.response.ErroResponse;
import com.cronorota.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private final ObjectMapper objectMapper;

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
            // permissão = 403 - é assim que o front-end distingue "sessão
            // expirou, faça login de novo" de "você não pode ver isto".
            // A resposta é escrita aqui mesmo, no formato ErroResponse: o
            // caminho padrão (sendError -> /error) passa de novo pela cadeia
            // de segurança SEM o JwtAuthFilter, e o 403 virava 401.
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) -> escreverErro(res, HttpStatus.UNAUTHORIZED, "Sessão ausente ou expirada"))
                    .accessDeniedHandler((req, res, e) -> escreverErro(res, HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para esta operação")))
            .authorizeHttpRequests(auth -> auth
                // Login sempre aberto - é a única forma de conseguir um token.
                .requestMatchers("/api/auth/**").permitAll()
                // Página de erro do Spring: sem isto, um 500 inesperado era
                // reencaminhado para /error, barrado como anônimo e chegava
                // ao front-end como 401 - deslogando o usuário por engano.
                .requestMatchers("/error").permitAll()
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
                // UC10: o dashboard é do gerente (e do administrador, que vê tudo).
                // O histórico (UC09) fica aberto aos três perfis, filtrado no service.
                .requestMatchers("/api/dashboard/**").hasAnyAuthority("ROLE_GERENTE", "ROLE_ADMINISTRADOR")
                // RNF05: a trilha completa é do administrador. As alterações de
                // um roteiro (/api/roteiros/{id}/alteracoes) seguem a RN13 do roteiro.
                .requestMatchers("/api/auditoria/**").hasAuthority("ROLE_ADMINISTRADOR")

                // O resto (consultar roteiro, registrar chegada/saída) vale
                // para qualquer perfil autenticado - quem pode ver QUAL
                // roteiro (RN13) é decidido no RoteiroService.verificarAcesso,
                // porque depende do dono do registro, não só do perfil.
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void escreverErro(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErroResponse.of(status.value(), mensagem));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Sem isto o navegador esconde do JavaScript o nome do arquivo que a
        // exportação (UC14) manda no Content-Disposition.
        config.setExposedHeaders(List.of("Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
