package com.cronorota.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// DTO = o formato que a API expõe pra fora, separado da entidade JPA.
// Por que não usar a entidade Motorista direto no @RequestBody? Porque o
// Motorista tem campos que o cliente da API nunca deveria enviar (id,
// senhaHash, ativo) - o DTO expõe só o que faz sentido pedir no cadastro.
public record CadastrarMotoristaRequest(

        @NotBlank String nome,
        @NotBlank String telefone,
        @Email @NotBlank String email,
        @NotBlank String documento,
        @NotBlank String habilitacao,
        @NotBlank String login,
        @NotBlank String senha,
        // Sem gerenteId: o motorista entra na equipe do gerente logado (token).

        @NotBlank String placaVeiculo,
        @NotBlank String modeloVeiculo,
        @NotBlank String tipoVeiculo,

        // RN10 também é validada aqui, na borda da API - além de na regra de
        // negócio no service. As duas camadas de validação têm papéis
        // diferentes: esta aqui barra o request cedo (HTTP 400, mensagem
        // amigável); a do service protege contra chamadas internas que não
        // passam por essa validação de borda.
        @NotNull @DecimalMin(value = "0.01", message = "Rendimento km/litro deve ser maior que zero")
        BigDecimal rendimentoKmLitro
) {
}
