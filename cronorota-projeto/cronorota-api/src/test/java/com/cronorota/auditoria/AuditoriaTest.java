package com.cronorota.auditoria;

import com.cronorota.dto.response.AlteracaoResponse.Campo;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Endereco;
import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import com.cronorota.security.UsuarioAutenticado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditoriaTest {

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void revisaoRecebeOUsuarioLogado() {
        var usuario = new UsuarioAutenticado(10L, "m1", "MOTORISTA", "Carlos");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));

        Revisao revisao = new Revisao();
        new AutorRevisaoListener().newRevision(revisao);

        assertThat(revisao.getUsuarioId()).isEqualTo(10L);
        assertThat(revisao.getLogin()).isEqualTo("m1");
        assertThat(revisao.getPerfil()).isEqualTo("MOTORISTA");
    }

    @Test
    void semUsuarioLogadoARevisaoFicaComoSistema() {
        Revisao revisao = new Revisao();
        new AutorRevisaoListener().newRevision(revisao);

        assertThat(revisao.getUsuarioId()).isNull();
        assertThat(revisao.getLogin()).isEqualTo(AutorRevisaoListener.SEM_LOGIN);
        assertThat(revisao.getPerfil()).isEqualTo(AutorRevisaoListener.PERFIL_SISTEMA);
    }

    @Test
    void diferencasMostraSoOQueMudou() {
        List<Campo> campos = AuditoriaService.diferencas(
                mapa("Chegada", "24/09/2026 09:00", "Saída", null, "Ordem", "2"),
                mapa("Chegada", "24/09/2026 09:00", "Saída", "24/09/2026 09:15", "Ordem", "2"));

        assertThat(campos).containsExactly(new Campo("Saída", null, "24/09/2026 09:15"));
    }

    @Test
    void inclusaoEremocao() {
        Map<String, String> ponto = mapa("Ordem", "2", "Saída", null);

        assertThat(AuditoriaService.diferencas(Map.of(), ponto)).containsExactly(new Campo("Ordem", null, "2"));
        assertThat(AuditoriaService.diferencas(ponto, Map.of())).containsExactly(new Campo("Ordem", "2", null));
    }

    @Test
    void descritorDoPontoFormataNoFusoDaOperacao() {
        Roteiro roteiro = Roteiro.builder().id(7L).build();
        Ponto ponto = Ponto.builder().id(1L).ordem(2).roteiro(roteiro).endereco(Endereco.builder().id(3L).build())
                .dataHoraChegada(OffsetDateTime.of(2026, 9, 24, 12, 0, 0, 0, ZoneOffset.UTC))
                .tempoParadoMinutos(15).build();

        Map<String, String> campos = DescritoresAuditoria.PONTO.camposDe(ponto);

        assertThat(campos).containsEntry("Roteiro", "#7")
                .containsEntry("Endereço", "#3")
                .containsEntry("Chegada", "24/09/2026 09:00") // 12:00 UTC = 09:00 em Brasília
                .containsEntry("Saída", null)
                .containsEntry("Tempo parado (min)", "15");
    }

    @Test
    void entidadeDesconhecidaNoFiltroEhRecusada() {
        assertThat(DescritoresAuditoria.porChave("ponto")).isSameAs(DescritoresAuditoria.PONTO);
        assertThatThrownBy(() -> DescritoresAuditoria.porChave("senha")).isInstanceOf(RegraDeNegocioException.class);
    }

    private static Map<String, String> mapa(String... rotuloValor) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (int i = 0; i < rotuloValor.length; i += 2) {
            mapa.put(rotuloValor[i], rotuloValor[i + 1]);
        }
        return mapa;
    }
}
