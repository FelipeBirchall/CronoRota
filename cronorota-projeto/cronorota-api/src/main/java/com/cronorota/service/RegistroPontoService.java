package com.cronorota.service;

import com.cronorota.exception.RecursoNaoEncontradoException;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Ponto;
import com.cronorota.repository.PontoRepository;
import com.cronorota.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

/**
 * Implementa o UC07 (Registrar chegada e saída no ponto). O relacionamento
 * «include» com UC08 do documento (seção 9.2: "todo registro de saída
 * dispara obrigatoriamente o cálculo do tempo parado") é literalmente isto
 * aqui: registrarSaida() chama o TempoParadoService antes de retornar.
 */
@Service
@RequiredArgsConstructor
public class RegistroPontoService {

    private final PontoRepository pontoRepository;
    private final RoteiroService roteiroService;
    private final TempoParadoService tempoParadoService;
    private final IndicadoresRoteiroService indicadoresRoteiroService;

    @Transactional
    public Ponto registrarChegada(Long pontoId, OffsetDateTime dataHoraChegada, UsuarioAutenticado usuario) {
        Ponto ponto = buscarPontoParaRegistro(pontoId, usuario);

        // UC07-A1: no ponto de partida o motorista só registra a saída - ele
        // já está lá quando o roteiro começa, então não existe "chegada".
        if (ehPontoDePartida(ponto)) {
            throw new RegraDeNegocioException("No ponto de partida registre apenas a saída");
        }

        // UC07-E3: chegada já registrada para o ponto.
        if (ponto.getDataHoraChegada() != null) {
            throw new RegraDeNegocioException("Chegada já registrada para este ponto");
        }

        ponto.setDataHoraChegada(dataHoraChegada);
        return pontoRepository.save(ponto);
    }

    @Transactional
    public Ponto registrarSaida(Long pontoId, OffsetDateTime dataHoraSaida, UsuarioAutenticado usuario) {
        Ponto ponto = buscarPontoParaRegistro(pontoId, usuario);

        if (ponto.getDataHoraSaida() != null) {
            throw new RegraDeNegocioException("Saída já registrada para este ponto");
        }

        if (!ehPontoDePartida(ponto)) {
            if (ponto.getDataHoraChegada() == null) {
                throw new RegraDeNegocioException("Não é possível registrar saída sem chegada prévia");
            }
            // UC07-E1: saída anterior à chegada. Sem esta checagem, o tempo
            // parado do ponto ficaria negativo e reduziria o total do roteiro.
            if (dataHoraSaida.isBefore(ponto.getDataHoraChegada())) {
                throw new RegraDeNegocioException("A saída não pode ser anterior à chegada");
            }
        }

        ponto.setDataHoraSaida(dataHoraSaida);

        // «include» UC08: calcula o tempo parado deste ponto...
        ponto.setTempoParadoMinutos(tempoParadoService.calcularTempoParadoDoPonto(ponto));
        pontoRepository.save(ponto);

        // ...e recalcula os totalizadores do roteiro inteiro (RN03, RN04),
        // já que um ponto novo sempre muda a soma.
        indicadoresRoteiroService.recalcular(ponto.getRoteiro());

        return ponto;
    }

    private boolean ehPontoDePartida(Ponto ponto) {
        return ponto.getOrdem() != null && ponto.getOrdem() == 1;
    }

    private Ponto buscarPontoParaRegistro(Long pontoId, UsuarioAutenticado usuario) {
        Ponto ponto = pontoRepository.findById(pontoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto não encontrado: " + pontoId));

        // RN13: só registra quem pode ver o roteiro (o próprio motorista, o
        // gerente da equipe dele ou o administrador).
        roteiroService.verificarAcesso(ponto.getRoteiro(), usuario);

        if (!ponto.getRoteiro().isAtivo()) {
            throw new RegraDeNegocioException("Este roteiro está inativo e não aceita novos registros");
        }
        return ponto;
    }
}
