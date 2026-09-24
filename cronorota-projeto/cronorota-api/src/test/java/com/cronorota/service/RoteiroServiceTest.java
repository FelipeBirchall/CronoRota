package com.cronorota.service;

import com.cronorota.exception.AcessoNegadoException;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Endereco;
import com.cronorota.model.Gerente;
import com.cronorota.model.Motorista;
import com.cronorota.model.Pedido;
import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import com.cronorota.model.SituacaoPedido;
import com.cronorota.repository.GerenteRepository;
import com.cronorota.repository.MotoristaRepository;
import com.cronorota.repository.PedidoRepository;
import com.cronorota.repository.RoteiroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.cronorota.service.Fixtures.DATA;
import static com.cronorota.service.Fixtures.endereco;
import static com.cronorota.service.Fixtures.gerente;
import static com.cronorota.service.Fixtures.motorista;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoteiroServiceTest {

    @Mock RoteiroRepository roteiroRepository;
    @Mock MotoristaRepository motoristaRepository;
    @Mock GerenteRepository gerenteRepository;
    @Mock PedidoRepository pedidoRepository;
    @InjectMocks RoteiroService service;

    private final Gerente gerente = gerente(1L);
    private Motorista motorista;

    @BeforeEach
    void setUp() {
        motorista = motorista(10L, gerente);
        motorista.setAtivo(true);
        when(gerenteRepository.findById(1L)).thenReturn(Optional.of(gerente));
        when(motoristaRepository.findById(10L)).thenReturn(Optional.of(motorista));
        when(roteiroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void montaPontosNaOrdemDosPedidos_RN06() {
        pedido(100L, endereco(1));
        pedido(101L, endereco(2));
        pedido(102L, endereco(3));

        Roteiro roteiro = montar(List.of(100L, 101L, 102L));

        assertThat(roteiro.getPontos()).extracting(Ponto::getOrdem).containsExactly(1, 2, 3);
        assertThat(roteiro.getPontos()).extracting(p -> p.getEndereco().getId()).containsExactly(1L, 2L, 3L);
    }

    @Test
    void pedidosNoMesmoEnderecoViramUmPontoSo() {
        Endereco mesmo = endereco(2);
        pedido(100L, endereco(1));
        Pedido a = pedido(101L, mesmo);
        Pedido b = pedido(102L, mesmo);

        Roteiro roteiro = montar(List.of(100L, 101L, 102L));

        assertThat(roteiro.getPontos()).hasSize(2);
        assertThat(a.getPonto()).isSameAs(b.getPonto());
    }

    @Test
    void menosDeDoisEnderecosDiferentesEhRecusado() {
        Endereco mesmo = endereco(1);
        pedido(100L, mesmo);
        pedido(101L, mesmo);

        assertThatThrownBy(() -> montar(List.of(100L, 101L))).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void motoristaJaTemRoteiroNaData_RN05() {
        when(roteiroRepository.existsByMotorista_IdAndDataAndAtivoTrue(10L, DATA)).thenReturn(true);
        pedido(100L, endereco(1));
        pedido(101L, endereco(2));

        assertThatThrownBy(() -> montar(List.of(100L, 101L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já possui um roteiro");
    }

    @Test
    void pedidoJaEmOutroRoteiroEhRecusado() {
        pedido(100L, endereco(1));
        pedido(101L, endereco(2)).setSituacao(SituacaoPedido.EM_ROTEIRO);

        assertThatThrownBy(() -> montar(List.of(100L, 101L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já está em outro roteiro");
    }

    @Test
    void pedidoDeOutraDataEhRecusado_RN12() {
        pedido(100L, endereco(1));
        pedido(101L, endereco(2)).setDataPrevista(DATA.plusDays(1));

        assertThatThrownBy(() -> montar(List.of(100L, 101L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("fora da data");
    }

    @Test
    void motoristaDeOutraEquipeEhRecusado_RN13() {
        motorista.setGerente(gerente(2L));
        pedido(100L, endereco(1));
        pedido(101L, endereco(2));

        assertThatThrownBy(() -> montar(List.of(100L, 101L))).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void motoristaInativoEhRecusado() {
        motorista.setAtivo(false);
        pedido(100L, endereco(1));
        pedido(101L, endereco(2));

        assertThatThrownBy(() -> montar(List.of(100L, 101L))).isInstanceOf(RegraDeNegocioException.class);
    }

    private Roteiro montar(List<Long> pedidoIds) {
        return service.montar(1L, 10L, DATA, pedidoIds, new BigDecimal("48"));
    }

    private Pedido pedido(long id, Endereco endereco) {
        Pedido pedido = Pedido.builder().id(id).codigo("P" + id).enderecoEntrega(endereco)
                .dataPrevista(DATA).situacao(SituacaoPedido.PENDENTE).build();
        when(pedidoRepository.findById(id)).thenReturn(Optional.of(pedido));
        return pedido;
    }
}
