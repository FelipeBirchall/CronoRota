package com.cronorota.dto.response;

import com.cronorota.model.Motorista;

// Assim como no request, o response também não expõe a entidade direto -
// aqui o motivo principal é nunca vazar o senhaHash pra fora da API.
public record MotoristaResponse(
        Long id,
        String nome,
        String telefone,
        String email,
        String documento,
        String placaVeiculo,
        Long gerenteId,
        boolean ativo
) {
    // Método de fábrica: converte a entidade JPA no DTO de saída. Mantém essa
    // conversão num lugar só, em vez de espalhada pelos controllers.
    public static MotoristaResponse fromEntity(Motorista motorista) {
        return new MotoristaResponse(
                motorista.getId(),
                motorista.getNome(),
                motorista.getTelefone(),
                motorista.getEmail(),
                motorista.getDocumento(),
                motorista.getVeiculo().getPlaca(),
                motorista.getGerente().getId(),
                motorista.isAtivo()
        );
    }
}
