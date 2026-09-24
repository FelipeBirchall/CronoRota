package com.cronorota.service;

import com.cronorota.auditoria.JustificativaAuditoria;
import com.cronorota.exception.AcessoNegadoException;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import com.cronorota.repository.PontoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static com.cronorota.service.Fixtures.as;
import static com.cronorota.service.Fixtures.gerente;
import static com.cronorota.service.Fixtures.logado;
import static com.cronorota.service.Fixtures.motorista;
import static com.cronorota.service.Fixtures.roteiro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// UC07-A3 (ajuste manual com justificativa) e UC08-A2 (recálculo).
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AjusteHorarioServiceTest {

    private static final String MOTIVO = "Motorista esqueceu de registrar a saída";
    // "Agora" nos testes: 24/09/2026 às 20:00 em Brasília.
    private static final Clock RELOGIO = Clock.fixed(as(20, 0).toInstant(), ZoneId.of("America/Sao_Paulo"));

    @Mock PontoRepository pontoRepository;
    @Mock IndicadoresRoteiroService indicadoresRoteiroService;
    @Mock JustificativaAuditoria justificativaAuditoria;

    private AjusteHorarioService service;
    private Roteiro roteiro;

    @BeforeEach
    void setUp() {
        service = new AjusteHorarioService(pontoRepository, new RoteiroService(null, null, null, null),
                new TempoParadoService(), indicadoresRoteiroService, justificativaAuditoria, RELOGIO);
        roteiro = roteiro(motorista(10L, gerente(1L)), 3);
        roteiro.getPontos().forEach(p -> when(pontoRepository.findById(p.getId())).thenReturn(Optional.of(p)));
        when(pontoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Ponto ponto(int ordem) {
        return roteiro.getPontos().get(ordem - 1);
    }

    // Horário local digitado pelo gerente, em Brasília.
    private static LocalDateTime local(int hora, int minuto) {
        return as(hora, minuto).toLocalDateTime();
    }

    @Test
    void gerentePreencheSaidaEsquecidaERecalcula() {
        ponto(2).setDataHoraChegada(as(9, 0)); // em atendimento: motorista não registrou a saída

        Ponto ajustado = service.ajustar(2L, null, local(9, 20), MOTIVO, logado("GERENTE", 1L));

        assertThat(ajustado.getDataHoraSaida()).isEqualTo(as(9, 20));
        assertThat(ajustado.getTempoParadoMinutos()).isEqualTo(20);
        verify(indicadoresRoteiroService).recalcular(roteiro);
        verify(justificativaAuditoria).registrar(MOTIVO);
    }

    @Test
    void corrigeChegadaEManteASaida() {
        ponto(2).setDataHoraChegada(as(9, 0));
        ponto(2).setDataHoraSaida(as(9, 50));

        Ponto ajustado = service.ajustar(2L, local(9, 30), null, MOTIVO, logado("GERENTE", 1L));

        assertThat(ajustado.getDataHoraSaida()).isEqualTo(as(9, 50));
        assertThat(ajustado.getTempoParadoMinutos()).isEqualTo(20);
    }

    @Test
    void justificativaEhObrigatoria() {
        ponto(2).setDataHoraChegada(as(9, 0));

        assertThatThrownBy(() -> service.ajustar(2L, null, local(9, 20), "   ok    ", logado("GERENTE", 1L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("justificativa");
        verify(pontoRepository, never()).save(any());
    }

    @Test
    void motoristaNaoAjusta() {
        assertThatThrownBy(() -> service.ajustar(2L, local(9, 0), null, MOTIVO, logado("MOTORISTA", 10L)))
                .isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(justificativaAuditoria);
    }

    @Test
    void gerenteDeOutraEquipeNaoAjusta_RN13() {
        assertThatThrownBy(() -> service.ajustar(2L, local(9, 0), null, MOTIVO, logado("GERENTE", 2L)))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void saidaAntesDaChegadaEhRecusada_UC07E1() {
        ponto(2).setDataHoraChegada(as(9, 0));

        assertThatThrownBy(() -> service.ajustar(2L, null, local(8, 50), MOTIVO, logado("GERENTE", 1L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("anterior à chegada");
    }

    @Test
    void horarioNoFuturoEhRecusado() {
        ponto(2).setDataHoraChegada(as(9, 0));

        assertThatThrownBy(() -> service.ajustar(2L, null, local(21, 0), MOTIVO, logado("GERENTE", 1L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void partidaSoTemSaida_UC07A1() {
        assertThatThrownBy(() -> service.ajustar(1L, local(8, 0), null, MOTIVO, logado("GERENTE", 1L)))
                .isInstanceOf(RegraDeNegocioException.class);

        Ponto partida = service.ajustar(1L, null, local(8, 0), MOTIVO, logado("GERENTE", 1L));
        assertThat(partida.getTempoParadoMinutos()).isZero();
    }

    @Test
    void saidaSemChegadaEhRecusada() {
        assertThatThrownBy(() -> service.ajustar(2L, null, local(9, 0), MOTIVO, logado("GERENTE", 1L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("chegada");
    }

    @Test
    void ajusteSemMudancaEhRecusado() {
        ponto(2).setDataHoraChegada(as(9, 0));

        assertThatThrownBy(() -> service.ajustar(2L, local(9, 0), null, MOTIVO, logado("GERENTE", 1L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Nenhum horário");
    }

    @Test
    void roteiroInativoNaoAceitaAjuste() {
        roteiro.setAtivo(false);
        ponto(2).setDataHoraChegada(as(9, 0));

        assertThatThrownBy(() -> service.ajustar(2L, null, local(9, 20), MOTIVO, logado("ADMINISTRADOR", 99L)))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
