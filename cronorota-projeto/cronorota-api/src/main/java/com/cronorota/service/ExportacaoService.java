package com.cronorota.service;

import com.cronorota.auditoria.RegistroExportacao;
import com.cronorota.auditoria.RegistroExportacaoRepository;
import com.cronorota.dto.response.HistoricoResponse;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.relatorio.DadosRelatorio;
import com.cronorota.relatorio.FormatoRelatorio;
import com.cronorota.relatorio.RelatorioCsv;
import com.cronorota.relatorio.RelatorioPdf;
import com.cronorota.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Implementa o UC14 (Exportar relatório do período), que «estende» o UC09:
 * parte da MESMA consulta do histórico (HistoricoService), de modo que o
 * arquivo tenha exatamente o conteúdo apresentado em tela (pós-condição do
 * UC14), inclusive a ordenação escolhida.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportacaoService {

    // UC14-E1: acima disto o arquivo fica grande demais para ser útil (e o
    // PDF, pesado para gerar em memória) - o gerente é orientado a reduzir o
    // período. 5.000 paradas ≈ 30 motoristas × 12 pontos × 2 semanas.
    static final int LIMITE_LINHAS = 5000;

    private final HistoricoService historicoService;
    private final MotoristaService motoristaService;
    private final RegistroExportacaoRepository registroExportacaoRepository;
    private final Clock clock;

    public record ArquivoExportado(String nomeArquivo, String contentType, byte[] conteudo) {
    }

    @Transactional
    public ArquivoExportado exportar(UsuarioAutenticado usuario, LocalDate inicio, LocalDate fim, Long motoristaId,
                                     FormatoRelatorio formato, boolean ordenarPorMaiorTempo) {
        HistoricoResponse historico = historicoService.consultar(usuario, inicio, fim, motoristaId);

        if (historico.pontos().size() > LIMITE_LINHAS) {
            throw new RegraDeNegocioException("O período tem " + historico.pontos().size()
                    + " paradas, acima do limite de " + LIMITE_LINHAS + " para exportação. Reduza o período.");
        }

        // UC09-A3: mesma ordenação da tela.
        List<HistoricoResponse.Linha> linhas = ordenarPorMaiorTempo
                ? historico.pontos().stream()
                        .sorted(Comparator.comparingInt(HistoricoResponse.Linha::tempoParadoMinutos).reversed())
                        .toList()
                : historico.pontos();

        DadosRelatorio dados = new DadosRelatorio(historico, linhas, descreverFiltro(usuario, motoristaId),
                ZonedDateTime.now(clock));

        byte[] conteudo = switch (formato) {
            case CSV -> RelatorioCsv.gerar(dados);
            case PDF -> RelatorioPdf.gerar(dados);
        };

        // UC14 passo 5: registra a exportação na auditoria.
        registroExportacaoRepository.save(RegistroExportacao.builder()
                .instante(OffsetDateTime.now(clock))
                .usuarioId(usuario.id())
                .login(usuario.login())
                .perfil(usuario.perfil())
                .formato(formato.name())
                .periodoInicio(inicio)
                .periodoFim(fim)
                .motoristaId(motoristaId)
                .quantidadeLinhas(linhas.size())
                .build());
        log.info("Exportação {} do histórico por {} ({}): {} a {}, motorista={}, {} paradas",
                formato, usuario.login(), usuario.perfil(), inicio, fim, motoristaId, linhas.size());

        String nome = "cronorota-historico-" + inicio + "_" + fim + "." + formato.extensao();
        return new ArquivoExportado(nome, formato.contentType(), conteudo);
    }

    private String descreverFiltro(UsuarioAutenticado usuario, Long motoristaId) {
        if (usuario.isMotorista()) {
            return "Motorista: " + usuario.nome();
        }
        if (motoristaId != null) {
            // buscarPorId também confere a RN13 (gerente só vê a própria equipe).
            return "Motorista: " + motoristaService.buscarPorId(motoristaId, usuario).getNome();
        }
        return usuario.isGerente() ? "Toda a equipe" : "Todos os motoristas";
    }
}
