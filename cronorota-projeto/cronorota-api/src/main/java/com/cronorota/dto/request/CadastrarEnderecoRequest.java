package com.cronorota.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CadastrarEnderecoRequest(
        @NotBlank String logradouro,
        @NotBlank String bairro,
        @NotBlank String cidade,
        @NotBlank String uf,
        @NotBlank String cep
) {
}
