package com.cronorota.service;

import com.cronorota.auditoria.JustificativaAuditoria;
import com.cronorota.exception.AcessoNegadoException;
import com.cronorota.exception.RecursoNaoEncontradoException;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import com.cronorota.relatorio.Formatacao;
import com.cronorota.repository.PontoRepository;
import com.cronorota.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Implementa o UC07-A3 (ajuste manual: "o gerente corrige um horário
 * registrado indevidamente; a alteração exige justificativa e é gravada na
 * auditoria") e o UC08-A2 (recálculo por correção de horário).
 *
 * O horário anterior e o novo já vão para a trilha sozinhos (Ponto é
 * @Audited); este service só acrescenta a justificativa à mesma revisão.
 */
@Service
@RequiredArgsConstructor
public class AjusteHorarioService {

    static final int JUSTIFICATIVA_MINIMA = 10;

    private final PontoRepository pontoRepository;
    private final RoteiroService roteiroService;
    private final TempoParadoService tempoParadoService;
    private final IndicadoresRoteiroService indicadoresRoteiroService;
    private final JustificativaAuditoria justificativaAuditoria;
    private final Clock clock;

    /**
     * @param novaChegada nula = manter a chegada atual
     * @param novaSaida   nula = manter a saída atual
     */
    @Transactional
    public Ponto ajustar(Long pontoId, LocalDateTime novaChegada, LocalDateTime novaSaida, String justificativa,
                         UsuarioAutenticado usuario) {
        // Quem ajusta é o gerente (ou o administrador); o motorista registra,
        // mas não corrige o próprio registro.
        if (!usuario.isGerente() && !usuario.isAdministrador()) {
            throw new AcessoNegadoException("Só o gerente pode ajustar horários registrados");
        }
        String motivo = justificativa == null ? "" : justificativa.strip();
        if (motivo.length() < JUSTIFICATIVA_MINIMA) {
            throw new RegraDeNegocioException("Informe a justificativa do ajuste (mínimo de "
                    + JUSTIFICATIVA_MINIMA + " caracteres)");
        }

        Ponto ponto = pontoRepository.findById(pontoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto não encontrado: " + pontoId));
        Roteiro roteiro = ponto.getRoteiro();
        roteiroService.verificarAcesso(roteiro, usuario); // RN13: só a própria equipe
        if (!roteiro.isAtivo()) {
            throw new RegraDeNegocioException("Este roteiro está inativo e não aceita ajustes");
        }

        boolean partida = ponto.getOrdem() != null && ponto.getOrdem() == 1;
        // UC07-A1: o ponto de partida só tem saída.
        if (partida && novaChegada != null) {
            throw new RegraDeNegocioException("No ponto de partida só existe horário de saída");
        }

        OffsetDateTime chegada = novaChegada != null ? noFusoDaOperacao(novaChegada) : ponto.getDataHoraChegada();
        OffsetDateTime saida = novaSaida != null ? noFusoDaOperacao(novaSaida) : ponto.getDataHoraSaida();

        if (mesmoInstante(chegada, ponto.getDataHoraChegada()) && mesmoInstante(saida, ponto.getDataHoraSaida())) {
            throw new RegraDeNegocioException("Nenhum horário foi alterado");
        }
        if (!partida && saida != null && chegada == null) {
            throw new RegraDeNegocioException("Informe também o horário de chegada");
        }
        // UC07-E1 vale também para o ajuste.
        if (chegada != null && saida != null && saida.isBefore(chegada)) {
            throw new RegraDeNegocioException("A saída não pode ser anterior à chegada");
        }
        // Um ajuste corrige algo que já aconteceu - horário no futuro é engano de digitação.
        OffsetDateTime agora = OffsetDateTime.now(clock);
        if ((novaChegada != null && chegada.isAfter(agora)) || (novaSaida != null && saida.isAfter(agora))) {
            throw new RegraDeNegocioException("O horário ajustado não pode estar no futuro");
        }

        ponto.setDataHoraChegada(chegada);
        ponto.setDataHoraSaida(saida);
        // UC08-A2: refaz o tempo do ponto e os totais do roteiro.
        ponto.setTempoParadoMinutos(tempoParadoService.calcularTempoParadoDoPonto(ponto));
        pontoRepository.save(ponto);
        indicadoresRoteiroService.recalcular(roteiro);

        justificativaAuditoria.registrar(motivo);
        return ponto;
    }

    private static OffsetDateTime noFusoDaOperacao(LocalDateTime horario) {
        return horario.atZone(Formatacao.FUSO).toOffsetDateTime();
    }

    // Compara o instante, não a representação: 12:00Z e 09:00-03:00 são o mesmo horário.
    private static boolean mesmoInstante(OffsetDateTime a, OffsetDateTime b) {
        return a == null ? b == null : b != null && a.isEqual(b);
    }
}
