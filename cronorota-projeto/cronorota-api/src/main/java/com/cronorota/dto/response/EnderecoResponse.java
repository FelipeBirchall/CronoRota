package com.cronorota.dto.response;

import com.cronorota.model.Endereco;

public record EnderecoResponse(Long id, String logradouro, String bairro, String cidade, String uf, String cep) {
    public static EnderecoResponse fromEntity(Endereco e) {
        return new EnderecoResponse(e.getId(), e.getLogradouro(), e.getBairro(), e.getCidade(), e.getUf(), e.getCep());
    }

    // Endereço em uma linha, do jeito que as telas de roteiro, histórico e
    // dashboard exibem - num lugar só pra não divergir entre elas.
    public static String formatado(Endereco e) {
        return e.getLogradouro() + ", " + e.getBairro() + " - " + e.getCidade() + "/" + e.getUf();
    }
}
