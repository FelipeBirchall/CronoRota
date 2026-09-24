package com.cronorota.service;

import com.cronorota.exception.AcessoNegadoException;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Motorista;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistroPontoServiceTest {

    @Mock PontoRepository pontoRepository;
    @Mock IndicadoresRoteiroService indicadoresRoteiroService;

    private RegistroPontoService service;
    private Roteiro roteiro;
    private Motorista motorista;

    @BeforeEach
    void setUp() {
        // verificarAcesso não usa repositório, então o RoteiroService real
        // serve aqui com dependências nulas.
        RoteiroService roteiroService = new RoteiroService(null, null, null, null);
        service = new RegistroPontoService(pontoRepository, roteiroService, new TempoParadoService(), indicadoresRoteiroService);

        motorista = motorista(10L, gerente(1L));
        roteiro = roteiro(motorista, 3);
        roteiro.getPontos().forEach(p -> when(pontoRepository.findById(p.getId())).thenReturn(Optional.of(p)));
        when(pontoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void saidaAnteriorAChegadaEhRecusada_UC07E1() {
        service.registrarChegada(2L, as(10, 0), logado("MOTORISTA", 10L));

        assertThatThrownBy(() -> service.registrarSaida(2L, as(9, 50), logado("MOTORISTA", 10L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("anterior à chegada");
        verify(indicadoresRoteiroService, never()).recalcular(any());
    }

    @Test
    void saidaCalculaTempoERecalculaRoteiro_UC07UC08() {
        service.registrarChegada(2L, as(10, 0), logado("MOTORISTA", 10L));
        Ponto ponto = service.registrarSaida(2L, as(10, 15), logado("MOTORISTA", 10L));

        assertThat(ponto.getTempoParadoMinutos()).isEqualTo(15);
        verify(indicadoresRoteiroService).recalcular(roteiro);
    }

    @Test
    void pontoDePartidaRegistraSoASaida_UC07A1() {
        assertThatThrownBy(() -> service.registrarChegada(1L, as(8, 0), logado("MOTORISTA", 10L)))
                .isInstanceOf(RegraDeNegocioException.class);

        Ponto partida = service.registrarSaida(1L, as(8, 0), logado("MOTORISTA", 10L));
        assertThat(partida.getTempoParadoMinutos()).isZero();
    }

    @Test
    void saidaDuplicadaEhRecusada() {
        service.registrarSaida(1L, as(8, 0), logado("MOTORISTA", 10L));
        assertThatThrownBy(() -> service.registrarSaida(1L, as(8, 5), logado("MOTORISTA", 10L)))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void outroMotoristaNaoRegistraNoRoteiroAlheio_RN13() {
        assertThatThrownBy(() -> service.registrarChegada(2L, as(10, 0), logado("MOTORISTA", 99L)))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void gerenteDeOutraEquipeNaoRegistra_RN13() {
        assertThatThrownBy(() -> service.registrarChegada(2L, as(10, 0), logado("GERENTE", 2L)))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void gerenteDaEquipeRegistra() {
        Ponto ponto = service.registrarChegada(2L, as(10, 0), logado("GERENTE", 1L));
        assertThat(ponto.getDataHoraChegada()).isEqualTo(as(10, 0));
    }

    @Test
    void roteiroInativoNaoAceitaRegistro_RN09() {
        roteiro.setAtivo(false);
        assertThatThrownBy(() -> service.registrarChegada(2L, as(10, 0), logado("MOTORISTA", 10L)))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
