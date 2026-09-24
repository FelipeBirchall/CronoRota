package com.cronorota.service;

import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Parametro;
import com.cronorota.repository.ParametroRepository;
import com.cronorota.repository.RoteiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Implementa o UC11/UC12 (Parametrizar custos e regras de cálculo), com a
// regra de vigência RN14: cada alteração grava um parâmetro NOVO, encerra a
// vigência do anterior e recalcula os roteiros alcançados pela nova vigência.
@Service
@RequiredArgsConstructor
public class ParametroService {

    // UC12-E1: jornada entre 1 e 24 horas.
    static final int JORNADA_MINIMA_MINUTOS = 60;
    static final int JORNADA_MAXIMA_MINUTOS = 24 * 60;

    private final ParametroRepository parametroRepository;
    private final RoteiroRepository roteiroRepository;
    private final IndicadoresRoteiroService indicadoresRoteiroService;

    @Transactional
    public Parametro cadastrar(BigDecimal valorCombustivel, Integer jornadaPadraoMinutos, LocalDate dataInicioVigencia) {
        // RN10: valor do combustível estritamente maior que zero.
        if (valorCombustivel == null || valorCombustivel.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Valor do combustível deve ser maior que zero");
        }
        if (jornadaPadraoMinutos == null
                || jornadaPadraoMinutos < JORNADA_MINIMA_MINUTOS
                || jornadaPadraoMinutos > JORNADA_MAXIMA_MINUTOS) {
            throw new RegraDeNegocioException("A jornada padrão deve ficar entre 1 e 24 horas");
        }

        // RN14 / UC11-E2: a nova vigência não pode começar junto ou antes de
        // um parâmetro já cadastrado - isso reescreveria um período cuja
        // regra já foi aplicada (ou já está agendada, no caso de vigência futura).
        if (parametroRepository.existsByDataInicioVigenciaGreaterThanEqual(dataInicioVigencia)) {
            throw new RegraDeNegocioException("Já existe parâmetro com vigência a partir de "
                    + dataInicioVigencia + " ou depois - escolha uma data posterior");
        }

        // RN14: encerra a vigência anterior no dia anterior ao início da nova.
        // saveAndFlush (e não save): no flush o Hibernate executa os INSERTs
        // antes dos UPDATEs, então o parâmetro novo seria gravado enquanto o
        // anterior ainda está "em aberto" - e o índice único de vigente
        // (migração V2) recusaria a gravação.
        parametroRepository.findByDataFimVigenciaIsNull().ifPresent(anterior -> {
            anterior.setDataFimVigencia(dataInicioVigencia.minusDays(1));
            parametroRepository.saveAndFlush(anterior);
        });

        Parametro parametro = parametroRepository.save(Parametro.builder()
                .valorCombustivel(valorCombustivel)
                .jornadaPadraoMinutos(jornadaPadraoMinutos)
                .dataInicioVigencia(dataInicioVigencia)
                .build());

        // UC08-A1 / UC11 passo 8 / UC12 passo 7: refaz percentual da jornada
        // e custo dos roteiros que caem na nova vigência. Os de antes dela
        // ficam intocados - é o "não retroage" da RN14.
        roteiroRepository.findByDataGreaterThanEqualAndAtivoTrue(dataInicioVigencia)
                .forEach(indicadoresRoteiroService::recalcular);

        return parametro;
    }

    public List<Parametro> listarTodos() {
        return parametroRepository.findAll();
    }
}
