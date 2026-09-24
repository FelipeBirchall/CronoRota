package com.cronorota.service;

import com.cronorota.exception.RecursoNaoEncontradoException;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Parametro;
import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import com.cronorota.repository.ParametroRepository;
import com.cronorota.repository.PontoRepository;
import com.cronorota.repository.RoteiroRepository;
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
    private final RoteiroRepository roteiroRepository;
    private final ParametroRepository parametroRepository;
    private final TempoParadoService tempoParadoService;
    private final CustoService custoService;

    @Transactional
    public Ponto registrarChegada(Long pontoId, OffsetDateTime dataHoraChegada) {
        Ponto ponto = buscarPonto(pontoId);

        // UC07-E3: chegada já registrada para o ponto.
        if (ponto.getDataHoraChegada() != null) {
            throw new RegraDeNegocioException("Chegada já registrada para este ponto");
        }

        ponto.setDataHoraChegada(dataHoraChegada);
        return pontoRepository.save(ponto);
    }

    @Transactional
    public Ponto registrarSaida(Long pontoId, OffsetDateTime dataHoraSaida) {
        Ponto ponto = buscarPonto(pontoId);

        if (ponto.getDataHoraChegada() == null) {
            throw new RegraDeNegocioException("Não é possível registrar saída sem chegada prévia");
        }

        ponto.setDataHoraSaida(dataHoraSaida);

        // «include» UC08: calcula o tempo parado deste ponto...
        Integer tempoParado = tempoParadoService.calcularTempoParadoDoPonto(ponto);
        ponto.setTempoParadoMinutos(tempoParado);
        pontoRepository.save(ponto);

        // ...e recalcula os totalizadores do roteiro inteiro (RN03, RN04),
        // já que um ponto novo sempre muda a soma.
        recalcularRoteiro(ponto.getRoteiro());

        return ponto;
    }

    /**
     * Também chamado pelo fluxo alternativo A1 do UC08 (recálculo por
     * alteração de parâmetro) e A2 (recálculo por correção de horário) -
     * centralizar aqui evita duplicar essa lógica em outro lugar do código.
     */
    @Transactional
    public void recalcularRoteiro(Roteiro roteiro) {
        int totalMinutos = tempoParadoService.calcularTempoTotalDoRoteiro(roteiro);
        roteiro.setTempoTotalParadoMinutos(totalMinutos);

        // UC08-E3: nenhum parâmetro vigente - usa 480 min (8h) como padrão
        // e, no fluxo completo, isso deveria também sinalizar o administrador
        // (não implementado nesta etapa: ficaria como um evento/notificação).
        Parametro parametro = parametroRepository.buscarVigenteEm(roteiro.getData()).orElse(null);
        int jornadaPadrao = parametro != null ? parametro.getJornadaPadraoMinutos() : 480;

        roteiro.setPercentualJornada(tempoParadoService.calcularPercentualJornada(totalMinutos, jornadaPadrao));

        if (parametro != null && roteiro.getDistanciaTotalKm() != null
                && roteiro.getMotorista().getVeiculo().getRendimentoKmLitro() != null) {
            roteiro.setCustoEstimado(custoService.calcularCustoEstimado(
                    roteiro.getDistanciaTotalKm(),
                    parametro,
                    roteiro.getMotorista().getVeiculo().getRendimentoKmLitro()
            ));
        }

        roteiroRepository.save(roteiro);
    }

    private Ponto buscarPonto(Long pontoId) {
        return pontoRepository.findById(pontoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto não encontrado: " + pontoId));
    }
}
