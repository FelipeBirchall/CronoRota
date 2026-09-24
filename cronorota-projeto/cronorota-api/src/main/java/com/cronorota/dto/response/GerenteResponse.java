package com.cronorota.dto.response;

import com.cronorota.model.Gerente;

public record GerenteResponse(Long id, String nome, String email) {
    public static GerenteResponse fromEntity(Gerente g) {
        return new GerenteResponse(g.getId(), g.getNome(), g.getEmail());
    }
}
