package com.cronorota.service;

import com.cronorota.dto.response.EnderecoResponse;
import com.cronorota.dto.response.HistoricoResponse;
import com.cronorota.model.Roteiro;
import com.cronorota.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

// Implementa o UC09 (Consultar histórico de tempos parados).
@Service
@RequiredArgsConstructor
public class HistoricoService {

    private final RoteiroService roteiroService;
    private final TempoParadoService tempoParadoService;

    @Transactional(readOnly = true)
    public HistoricoResponse consultar(UsuarioAutenticado usuario, LocalDate inicio, LocalDate fim, Long motoristaId) {
        List<Roteiro> roteiros = roteiroService.buscarNoPeriodo(usuario, inicio, fim, motoristaId);

        List<HistoricoResponse.Linha> linhas = roteiros.stream()
                .flatMap(roteiro -> roteiro.getPontos().stream()
                        .filter(tempoParadoService::contaComoParada)
                        .map(ponto -> new HistoricoResponse.Linha(
                                roteiro.getId(),
                                roteiro.getData(),
                                roteiro.getMotorista().getId(),
                                roteiro.getMotorista().getNome(),
                                ponto.getOrdem(),
                                EnderecoResponse.formatado(ponto.getEndereco()),
                                ponto.getDataHoraChegada(),
                                ponto.getDataHoraSaida(),
                                ponto.getTempoParadoMinutos())))
                .toList();

        // UC09 passo 6: totalizadores do período.
        int total = linhas.stream().mapToInt(HistoricoResponse.Linha::tempoParadoMinutos).sum();

        return new HistoricoResponse(inicio, fim, roteiros.size(), linhas.size(), total,
                media(total, roteiros.size()), media(total, linhas.size()), linhas);
    }

    static BigDecimal media(int total, int quantidade) {
        if (quantidade == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(quantidade), 1, RoundingMode.HALF_UP);
    }
}
