package com.cronorota.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Gera e valida o token JWT (UC01). O token carrega três informações no
 * corpo (claims): o login do usuário (subject), o id dele e o perfil
 * (MOTORISTA/GERENTE/ADMINISTRADOR) - é a partir do perfil que o
 * JwtAuthFilter decide as permissões (RN13) a cada requisição, sem precisar
 * consultar o banco de novo.
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(
            @Value("${cronorota.jwt.secret}") String segredo,
            @Value("${cronorota.jwt.expiracao-horas:12}") long expiracaoHoras
    ) {
        // A chave precisa ter pelo menos 256 bits (32 caracteres) para o
        // algoritmo HS256 - se o segredo configurado for menor que isso, o
        // Keys.hmacShaKeyFor lança exceção na inicialização, o que é
        // proposital: preferível falhar cedo a gerar tokens fracos.
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes());
        this.expiracaoMs = expiracaoHoras * 60 * 60 * 1000;
    }

    public String gerarToken(String login, Long usuarioId, String perfil, String nome) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(login)
                .claim("usuarioId", usuarioId)
                .claim("perfil", perfil)
                .claim("nome", nome)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    /**
     * @return as claims do token, ou null se o token for inválido/expirado -
     * o chamador (JwtAuthFilter) decide o que fazer com isso, sem exceção
     * atravessando a cadeia de filtros do Spring Security.
     */
    public Claims validarToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }
}
