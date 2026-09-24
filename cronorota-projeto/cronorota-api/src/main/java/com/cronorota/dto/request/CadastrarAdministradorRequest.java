package com.cronorota.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CadastrarAdministradorRequest(
        @NotBlank String nome,
        @NotBlank String telefone,
        @Email @NotBlank String email,
        @NotBlank String login,
        @NotBlank String senha
) {
}
