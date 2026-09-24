package com.cronorota.security;

// Representa quem está logado na requisição atual - é isto que
// @AuthenticationPrincipal injeta nos controllers que precisam saber
// "qual é o id/perfil de quem está pedindo isso" (ex.: GET /roteiros/meus
// precisa saber o id do motorista logado, não recebido por parâmetro).
public record UsuarioAutenticado(Long id, String login, String perfil, String nome) {
}
