package com.cronorota.dto.response;

public record LoginResponse(String token, String perfil, Long usuarioId, String nome) {
}
