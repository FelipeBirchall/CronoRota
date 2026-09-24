package com.cronorota.service;

import com.cronorota.dto.response.DashboardResponse;
import com.cronorota.dto.response.DashboardResponse.Indicadores;
import com.cronorota.dto.response.DashboardResponse.PontoCritico;
import com.cronorota.dto.response.DashboardResponse.TotalDoEndereco;
import com.cronorota.dto.response.DashboardResponse.TotalDoMotorista;
import com.cronorota.dto.response.DashboardResponse.TotalDoRoteiro;
import com.cronorota.dto.response.DashboardResponse.TotalNoIntervalo;
import com.cronorota.dto.response.EnderecoResponse;
import com.cronorota.model.Ponto;
import com.cronorota.model.Roteiro;
import com.cronorota.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static com.cronorota.service.HistoricoService.media;

/**
 * Implementa o UC10 (Visualizar dashboard) - o "Serviço de Agregação do
 * Dashboard" da seção 26.2. Agrega em memória os roteiros do recorte, que
 * chegam do banco numa consulta só (RoteiroRepository.buscarNoPeriodo). No
 * volume do piloto (30 motoristas × 12 pontos/dia, seção 28.1), um ano dá
 * ~11 mil roteiros - folgado para o RNF03. O cache em Redis previsto no
 * documento fica para quando o volume pedir.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    static final int TAMANHO_RANKING = 10;

    private final RoteiroService roteiroService;
    private final TempoParadoService tempoParadoService;

    @Transactional(readOnly = true)
    public DashboardResponse montar(UsuarioAutenticado usuario, LocalDate inicio, LocalDate fim, Long motoristaId) {
        List<Roteiro> roteiros = roteiroService.buscarNoPeriodo(usuario, inicio, fim, motoristaId);

        return new DashboardResponse(
                inicio, fim,
                indicadores(roteiros),
                porDia(roteiros, inicio, fim),
                porMes(roteiros, inicio, fim),
                porRoteiro(roteiros),
                rankingEnderecos(roteiros),
                porMotorista(roteiros));
    }

    // RN03 aplicado ao recorte: soma só as paradas concluídas, fora a partida.
    private int minutosParados(Roteiro roteiro) {
        return paradas(roteiro).stream().mapToInt(Ponto::getTempoParadoMinutos).sum();
    }

    private List<Ponto> paradas(Roteiro roteiro) {
        return roteiro.getPontos().stream().filter(tempoParadoService::contaComoParada).toList();
    }

    private Indicadores indicadores(List<Roteiro> roteiros) {
        int total = roteiros.stream().mapToInt(this::minutosParados).sum();

        List<BigDecimal> percentuais = roteiros.stream()
                .map(Roteiro::getPercentualJornada).filter(Objects::nonNull).toList();
        BigDecimal percentualMedio = percentuais.isEmpty() ? null
                : percentuais.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(percentuais.size()), 2, RoundingMode.HALF_UP);

        // UC10-E3: sem custo em nenhum roteiro, o indicador é "não disponível".
        List<BigDecimal> custos = roteiros.stream().map(Roteiro::getCustoEstimado).filter(Objects::nonNull).toList();
        BigDecimal custoTotal = custos.isEmpty() ? null : custos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        PontoCritico critico = roteiros.stream()
                .flatMap(r -> paradas(r).stream())
                .max(Comparator.comparingInt(Ponto::getTempoParadoMinutos))
                .map(p -> new PontoCritico(p.getRoteiro().getId(), p.getRoteiro().getData(),
                        p.getRoteiro().getMotorista().getNome(),
                        EnderecoResponse.formatado(p.getEndereco()), p.getTempoParadoMinutos()))
                .orElse(null);

        return new Indicadores(roteiros.size(), total, percentualMedio, critico, custoTotal,
                !roteiros.isEmpty() && custos.size() == roteiros.size());
    }

    // Um item por dia do intervalo, inclusive os sem roteiro (zero) - senão
    // o gráfico "pularia" os dias parados e o eixo do tempo mentiria.
    private List<TotalNoIntervalo> porDia(List<Roteiro> roteiros, LocalDate inicio, LocalDate fim) {
        Map<String, int[]> totais = new LinkedHashMap<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            totais.put(dia.toString(), new int[2]);
        }
        return acumular(roteiros, totais, r -> r.getData().toString());
    }

    private List<TotalNoIntervalo> porMes(List<Roteiro> roteiros, LocalDate inicio, LocalDate fim) {
        Map<String, int[]> totais = new LinkedHashMap<>();
        for (YearMonth mes = YearMonth.from(inicio); !mes.isAfter(YearMonth.from(fim)); mes = mes.plusMonths(1)) {
            totais.put(mes.toString(), new int[2]);
        }
        return acumular(roteiros, totais, r -> YearMonth.from(r.getData()).toString());
    }

    // totais: chave -> [minutos, roteiros], já com todas as chaves do eixo.
    private List<TotalNoIntervalo> acumular(List<Roteiro> roteiros, Map<String, int[]> totais,
                                            Function<Roteiro, String> chave) {
        for (Roteiro roteiro : roteiros) {
            int[] acumulado = totais.get(chave.apply(roteiro));
            acumulado[0] += minutosParados(roteiro);
            acumulado[1]++;
        }
        List<TotalNoIntervalo> resultado = new ArrayList<>();
        totais.forEach((k, v) -> resultado.add(new TotalNoIntervalo(k, v[0], v[1])));
        return resultado;
    }

    private List<TotalDoRoteiro> porRoteiro(List<Roteiro> roteiros) {
        return roteiros.stream()
                .map(r -> new TotalDoRoteiro(r.getId(), r.getData(), r.getMotorista().getNome(),
                        minutosParados(r), r.getPercentualJornada()))
                .toList();
    }

    private List<TotalDoEndereco> rankingEnderecos(List<Roteiro> roteiros) {
        Map<Long, TotalDoEndereco> porEndereco = new LinkedHashMap<>();
        roteiros.stream().flatMap(r -> paradas(r).stream()).forEach(p -> porEndereco.merge(
                p.getEndereco().getId(),
                new TotalDoEndereco(p.getEndereco().getId(), EnderecoResponse.formatado(p.getEndereco()),
                        p.getTempoParadoMinutos(), 1),
                (a, b) -> new TotalDoEndereco(a.enderecoId(), a.endereco(),
                        a.tempoParadoMinutos() + b.tempoParadoMinutos(), a.paradas() + b.paradas())));

        return porEndereco.values().stream()
                .sorted(Comparator.comparingInt(TotalDoEndereco::tempoParadoMinutos).reversed())
                .limit(TAMANHO_RANKING)
                .toList();
    }

    private List<TotalDoMotorista> porMotorista(List<Roteiro> roteiros) {
        Map<Long, List<Roteiro>> agrupados = new LinkedHashMap<>();
        roteiros.forEach(r -> agrupados.computeIfAbsent(r.getMotorista().getId(), id -> new ArrayList<>()).add(r));

        return agrupados.values().stream()
                .map(lista -> {
                    int total = lista.stream().mapToInt(this::minutosParados).sum();
                    var motorista = lista.get(0).getMotorista();
                    return new TotalDoMotorista(motorista.getId(), motorista.getNome(), lista.size(), total,
                            media(total, lista.size()));
                })
                .sorted(Comparator.comparing(TotalDoMotorista::mediaPorRoteiroMinutos).reversed())
                .toList();
    }
}
