package com.cronorota.service;

import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Parametro;
import com.cronorota.model.Roteiro;
import com.cronorota.repository.ParametroRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParametroServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 10, 1);

    @Mock ParametroRepository parametroRepository;
    @Mock RoteiroRepository roteiroRepository;
    @Mock IndicadoresRoteiroService indicadoresRoteiroService;
    @InjectMocks ParametroService service;

    @BeforeEach
    void setUp() {
        when(parametroRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void novaVigenciaEncerraAAnteriorNoDiaAnterior_RN14() {
        Parametro anterior = Parametro.builder().valorCombustivel(new BigDecimal("5.80"))
                .dataInicioVigencia(LocalDate.of(2026, 9, 1)).build();
        when(parametroRepository.findByDataFimVigenciaIsNull()).thenReturn(Optional.of(anterior));

        service.cadastrar(new BigDecimal("6.00"), 480, INICIO);

        assertThat(anterior.getDataFimVigencia()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void vigenciaSobrepostaEhRecusada_UC11E2() {
        when(parametroRepository.existsByDataInicioVigenciaGreaterThanEqual(INICIO)).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(new BigDecimal("6.00"), 480, INICIO))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(parametroRepository, never()).save(any());
    }

    @Test
    void recalculaSoOsRoteirosDaNovaVigencia() {
        Roteiro alcancado = Roteiro.builder().data(INICIO).build();
        when(roteiroRepository.findByDataGreaterThanEqualAndAtivoTrue(INICIO)).thenReturn(List.of(alcancado));

        service.cadastrar(new BigDecimal("6.00"), 480, INICIO);

        verify(indicadoresRoteiroService).recalcular(alcancado);
    }

    @Test
    void jornadaForaDaFaixaEhRecusada_UC12E1() {
        assertThatThrownBy(() -> service.cadastrar(new BigDecimal("6.00"), 30, INICIO))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.cadastrar(new BigDecimal("6.00"), 1500, INICIO))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
