package com.cronorota.service;

import com.cronorota.model.Parametro;
import com.cronorota.model.Roteiro;
import com.cronorota.repository.ParametroRepository;
import com.cronorota.repository.RoteiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalcula os totalizadores gravados no Roteiro (tempo total parado,
 * percentual da jornada e custo estimado). Fica num service próprio porque
 * tem mais de um gatilho: o registro de saída num ponto (UC07 «include»
 * UC08) e a entrada em vigência de um novo parâmetro (UC08-A1, UC11, UC12) -
 * centralizar aqui evita duplicar essa lógica nos dois lugares.
 */
@Service
@RequiredArgsConstructor
public class IndicadoresRoteiroService {

    // UC08-E3: sem parâmetro vigente, vale a jornada padrão do MVP (8h).
    static final int JORNADA_PADRAO_MINUTOS = 480;

    private final RoteiroRepository roteiroRepository;
    private final ParametroRepository parametroRepository;
    private final TempoParadoService tempoParadoService;
    private final CustoService custoService;

    @Transactional
    public void recalcular(Roteiro roteiro) {
        int totalMinutos = tempoParadoService.calcularTempoTotalDoRoteiro(roteiro);
        roteiro.setTempoTotalParadoMinutos(totalMinutos);

        // RN14: usa o parâmetro vigente na data DO ROTEIRO, não o de hoje.
        // No fluxo completo de UC08-E3, a ausência de parâmetro também
        // sinalizaria o administrador (ainda não implementado).
        Parametro parametro = parametroRepository.buscarVigenteEm(roteiro.getData()).orElse(null);
        int jornadaPadrao = parametro != null ? parametro.getJornadaPadraoMinutos() : JORNADA_PADRAO_MINUTOS;

        roteiro.setPercentualJornada(tempoParadoService.calcularPercentualJornada(totalMinutos, jornadaPadrao));

        // UC13-E1: sem distância ou sem parâmetro de custo, o custo fica
        // "não disponível" (nulo) em vez de um valor inventado.
        var rendimento = roteiro.getMotorista().getVeiculo().getRendimentoKmLitro();
        if (parametro != null && roteiro.getDistanciaTotalKm() != null && rendimento != null) {
            roteiro.setCustoEstimado(custoService.calcularCustoEstimado(roteiro.getDistanciaTotalKm(), parametro, rendimento));
        } else {
            roteiro.setCustoEstimado(null);
        }

        roteiroRepository.save(roteiro);
    }
}
