package com.cronorota.dto.response;

import com.cronorota.model.Administrador;

public record AdministradorResponse(Long id, String nome, String email) {
    public static AdministradorResponse fromEntity(Administrador a) {
        return new AdministradorResponse(a.getId(), a.getNome(), a.getEmail());
    }
}
