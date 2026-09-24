package com.cronorota.security;

import com.cronorota.security.UsuarioAutenticado;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/**
 * Roda uma vez por requisição, antes de qualquer controller. Lê o cabeçalho
 * "Authorization: Bearer <token>", valida com o JwtService e, se for
 * válido, registra o usuário autenticado no contexto de segurança do
 * Spring - é isso que faz @AuthenticationPrincipal e
 * .authorizeHttpRequests(...).hasAuthority("ROLE_X") funcionarem depois.
 *
 * Se não houver token, ou o token for inválido, o filtro simplesmente não
 * autentica ninguém e segue a cadeia - quem decide se isso é um problema é
 * o SecurityConfig (uma rota marcada como permitAll funciona sem token; uma
 * marcada como authenticated() é rejeitada mais adiante, com 401/403).
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Claims claims = jwtService.validarToken(token);

            if (claims != null) {
                String perfil = claims.get("perfil", String.class);
                Long usuarioId = claims.get("usuarioId", Long.class);
                String nome = claims.get("nome", String.class);
                String login = claims.getSubject();

                UsuarioAutenticado principal = new UsuarioAutenticado(usuarioId, login, perfil, nome);
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + perfil));

                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
