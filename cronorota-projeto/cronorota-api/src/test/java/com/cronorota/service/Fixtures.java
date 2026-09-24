package com.cronorota.service;

import com.cronorota.model.Endereco;
import com.cronorota.model.Gerente;
import com.cronorota.model.Motorista;
import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import com.cronorota.model.Veiculo;
import com.cronorota.security.UsuarioAutenticado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

// Montagem de objetos de domínio para os testes, sem banco.
final class Fixtures {

    static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);
    static final LocalDate DATA = LocalDate.of(2026, 9, 24);

    private Fixtures() {
    }

    static OffsetDateTime as(int hora, int minuto) {
        return OffsetDateTime.of(DATA.getYear(), DATA.getMonthValue(), DATA.getDayOfMonth(), hora, minuto, 0, 0, BRASILIA);
    }

    static Gerente gerente(long id) {
        return Gerente.builder().id(id).nome("Gerente " + id).build();
    }

    static Motorista motorista(long id, Gerente gerente) {
        return Motorista.builder()
                .id(id).nome("Motorista " + id)
                .gerente(gerente)
                .veiculo(Veiculo.builder().placa("ABC" + id).rendimentoKmLitro(new BigDecimal("12")).build())
                .build();
    }

    static Endereco endereco(long id) {
        return Endereco.builder().id(id).logradouro("Rua " + id).bairro("Centro").cidade("BH").uf("MG").cep("30000-000").build();
    }

    static Roteiro roteiro(Motorista motorista, int quantidadePontos) {
        Roteiro roteiro = Roteiro.builder().id(1L).data(DATA).motorista(motorista).ativo(true).pontos(new ArrayList<>()).build();
        for (int ordem = 1; ordem <= quantidadePontos; ordem++) {
            roteiro.getPontos().add(Ponto.builder().id((long) ordem).ordem(ordem).roteiro(roteiro).endereco(endereco(ordem)).build());
        }
        return roteiro;
    }

    static UsuarioAutenticado logado(String perfil, long id) {
        return new UsuarioAutenticado(id, "login" + id, perfil, "Usuário " + id);
    }
}
