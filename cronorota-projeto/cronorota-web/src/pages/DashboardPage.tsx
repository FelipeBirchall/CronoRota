import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { FiltroPeriodo, queryDoPeriodo, usePeriodo } from '../components/FiltroPeriodo';
import { GraficoBarras, type ItemGrafico } from '../components/GraficoBarras';
import { Card } from '../components/ui';
import type { Dashboard } from '../types';
import { formatarData, formatarMes, formatarMinutos } from '../utils/data';

type Recorte = 'dia' | 'mes' | 'periodo';

const RECORTES: { valor: Recorte; rotulo: string }[] = [
  { valor: 'dia', rotulo: 'Por dia' },
  { valor: 'mes', rotulo: 'Por mês' },
  { valor: 'periodo', rotulo: 'Por período' },
];

// Acima deste percentual da jornada o tempo parado é tratado como alerta -
// o mesmo patamar da tela de detalhe do roteiro.
const LIMITE_ALERTA_JORNADA = 20;

function plural(n: number, singular: string, pluralTexto: string) {
  return `${n} ${n === 1 ? singular : pluralTexto}`;
}

// UC10 - Visualizar dashboard. Um único filtro (período + motorista) define
// o conjunto de roteiros; indicadores, os três recortes, o ranking e a
// comparação entre motoristas saem todos dele (critério de aceitação da
// seção 18: "os mesmos dados de base").
export function DashboardPage() {
  const navigate = useNavigate();
  const [periodo, setPeriodo] = usePeriodo(30);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [recorte, setRecorte] = useState<Recorte>('dia');
  const [verTabela, setVerTabela] = useState(false);

  useEffect(() => {
    setErro(null);
    setCarregando(true);
    api.get<Dashboard>(`/dashboard?${queryDoPeriodo(periodo)}`)
      .then(setDashboard)
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, [periodo.inicio, periodo.fim, periodo.motoristaId]);

  const semDados = dashboard !== null && dashboard.indicadores.quantidadeRoteiros === 0;

  // Os três recortes viram a mesma forma de dado para o gráfico.
  const serie: ItemGrafico[] = !dashboard ? [] : recorte === 'dia'
    ? dashboard.porDia.map((d) => ({
        rotulo: formatarData(d.chave).slice(0, 5),
        detalhe: `${formatarData(d.chave)} · ${plural(d.quantidadeRoteiros, 'roteiro', 'roteiros')}`,
        minutos: d.tempoParadoMinutos,
      }))
    : recorte === 'mes'
      ? dashboard.porMes.map((m) => ({
          rotulo: formatarMes(m.chave),
          detalhe: `${formatarMes(m.chave)} · ${plural(m.quantidadeRoteiros, 'roteiro', 'roteiros')}`,
          minutos: m.tempoParadoMinutos,
        }))
      : dashboard.porRoteiro.map((r) => ({
          rotulo: `#${r.roteiroId}`,
          detalhe: `Roteiro #${r.roteiroId} · ${formatarData(r.data)} · ${r.motorista}`,
          minutos: r.tempoParadoMinutos,
        }));

  // UC10-A1: clicar numa barra abre o detalhe daquele recorte.
  function selecionar(indice: number) {
    if (!dashboard) return;
    const extra = periodo.motoristaId ? `&motoristaId=${periodo.motoristaId}` : '';
    if (recorte === 'dia') {
      const dia = dashboard.porDia[indice].chave;
      navigate(`/gerente/historico?inicio=${dia}&fim=${dia}${extra}`);
    } else if (recorte === 'mes') {
      const mes = dashboard.porMes[indice].chave;
      const inicio = mes + '-01' < periodo.inicio ? periodo.inicio : mes + '-01';
      const ultimoDia = new Date(Number(mes.slice(0, 4)), Number(mes.slice(5, 7)), 0).getDate();
      const fimMes = `${mes}-${String(ultimoDia).padStart(2, '0')}`;
      const fim = fimMes > periodo.fim ? periodo.fim : fimMes;
      navigate(`/gerente/historico?inicio=${inicio}&fim=${fim}${extra}`);
    } else {
      navigate(`/gerente/roteiros/${dashboard.porRoteiro[indice].roteiroId}`);
    }
  }

  const ind = dashboard?.indicadores;
  const percentual = ind?.percentualJornadaMedio ?? null;

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-xl font-semibold">Dashboard</h1>
        <p className="text-sm text-grafite mt-1">Tempo parado da sua equipe e custo estimado dos roteiros (UC10).</p>
      </div>

      <FiltroPeriodo periodo={periodo} onChange={setPeriodo} mostrarMotorista />

      {erro && <Card><p className="text-sm text-terracota">{erro}</p></Card>}

      {dashboard && ind && (
        // Ao trocar o filtro, o painel anterior fica esmaecido até chegar o
        // novo - sem "piscar" nem pular o layout.
        <div className={`space-y-5 transition-opacity ${carregando ? 'opacity-50' : ''}`}>
          {/* Painel de indicadores (UC10 passo 8) */}
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-5 gap-3">
            <div className="md:col-span-2 bg-white rounded-lg border border-neutral-200 p-5">
              <p className="text-xs text-grafite">Tempo total parado</p>
              <p className="text-5xl font-semibold mt-2 leading-none whitespace-nowrap">{formatarMinutos(ind.tempoTotalParadoMinutos)}</p>
              <p className="text-xs text-grafite mt-2">{plural(ind.quantidadeRoteiros, 'roteiro', 'roteiros')} no período</p>
            </div>

            <div className="bg-white rounded-lg border border-neutral-200 p-5">
              <p className="text-xs text-grafite">% da jornada (média por roteiro)</p>
              <p className="text-2xl font-semibold mt-2">{percentual !== null ? `${percentual.toFixed(1).replace('.', ',')}%` : '—'}</p>
              {percentual !== null && percentual > LIMITE_ALERTA_JORNADA && (
                <p className="text-xs font-medium text-terracota mt-1">▲ acima de {LIMITE_ALERTA_JORNADA}% da jornada</p>
              )}
            </div>

            <div className="bg-white rounded-lg border border-neutral-200 p-5">
              <p className="text-xs text-grafite">Ponto mais crítico</p>
              {ind.pontoMaisCritico ? (
                <Link to={`/gerente/roteiros/${ind.pontoMaisCritico.roteiroId}`} className="block group">
                  <p className="text-2xl font-semibold mt-2">{formatarMinutos(ind.pontoMaisCritico.tempoParadoMinutos)}</p>
                  <p className="text-sm leading-snug mt-1 group-hover:underline">{ind.pontoMaisCritico.endereco}</p>
                  <p className="text-xs text-grafite mt-0.5">
                    {formatarData(ind.pontoMaisCritico.data)} · {ind.pontoMaisCritico.motorista}
                  </p>
                </Link>
              ) : (
                <p className="text-2xl font-semibold mt-2">—</p>
              )}
            </div>

            <div className="bg-white rounded-lg border border-neutral-200 p-5">
              <p className="text-xs text-grafite">Custo estimado</p>
              {/* UC10-E3: sem parâmetro de custo, o indicador fica "não disponível". */}
              <p className="text-2xl font-semibold mt-2">
                {ind.custoEstimadoTotal !== null
                  ? ind.custoEstimadoTotal.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
                  : 'Não disponível'}
              </p>
              {ind.custoEstimadoTotal !== null && !ind.custoCompleto && (
                <p className="text-xs text-grafite mt-1">Parcial: há roteiros sem distância ou sem parâmetro de custo</p>
              )}
            </div>
          </div>

          <Card>
            <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
              <div className="flex rounded-md border border-neutral-300 overflow-hidden" role="tablist">
                {RECORTES.map((r) => (
                  <button
                    key={r.valor}
                    role="tab"
                    aria-selected={recorte === r.valor}
                    onClick={() => setRecorte(r.valor)}
                    className={`px-3 py-1.5 text-sm border-r border-neutral-200 last:border-r-0 ${
                      recorte === r.valor ? 'bg-petroleo text-white font-medium' : 'text-grafite hover:bg-neutral-50'
                    }`}
                  >
                    {r.rotulo}
                  </button>
                ))}
              </div>
              <div className="flex items-center gap-4">
                <p className="text-xs text-grafite">
                  {recorte === 'periodo' ? 'Clique num roteiro para abrir o detalhe' : 'Clique numa barra para ver o histórico'}
                </p>
                <button onClick={() => setVerTabela(!verTabela)} className="text-xs font-medium text-petroleo hover:underline">
                  {verTabela ? 'Ver gráfico' : 'Ver tabela'}
                </button>
              </div>
            </div>

            <h2 className="text-sm font-medium mb-2">
              Tempo parado {recorte === 'dia' ? 'por dia' : recorte === 'mes' ? 'por mês' : 'por roteiro do período'}
            </h2>

            {semDados ? (
              // UC10-E1
              <p className="py-16 text-center text-sm text-grafite">Sem registros no período.</p>
            ) : verTabela ? (
              <TabelaSerie serie={serie} />
            ) : (
              <GraficoBarras
                dados={serie}
                onSelecionar={selecionar}
                descricao={`Gráfico de barras do tempo parado ${recorte === 'dia' ? 'por dia' : recorte === 'mes' ? 'por mês' : 'por roteiro'}`}
              />
            )}
          </Card>

          {!semDados && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
              {/* UC10-A2 */}
              <Card>
                <h2 className="text-sm font-medium">Endereços que mais retêm a frota</h2>
                <p className="text-xs text-grafite mt-0.5 mb-3">Tempo parado acumulado no período, top {dashboard.rankingEnderecos.length}</p>
                <GraficoBarras
                  horizontal
                  altura={Math.max(120, dashboard.rankingEnderecos.length * 34)}
                  dados={dashboard.rankingEnderecos.map((e) => ({
                    // No eixo só a rua e o número; o endereço completo vai no tooltip.
                    rotulo: e.endereco.split(',').slice(0, 2).join(','),
                    detalhe: `${e.endereco} · ${plural(e.paradas, 'parada', 'paradas')}`,
                    minutos: e.tempoParadoMinutos,
                  }))}
                  descricao="Ranking dos endereços com maior tempo parado acumulado"
                />
              </Card>

              {/* UC10-A3 */}
              <Card>
                <h2 className="text-sm font-medium">Comparação entre motoristas</h2>
                <p className="text-xs text-grafite mt-0.5 mb-3">Tempo parado médio por roteiro</p>
                <GraficoBarras
                  horizontal
                  altura={Math.max(120, dashboard.porMotorista.length * 34)}
                  dados={dashboard.porMotorista.map((m) => ({
                    rotulo: m.motorista,
                    detalhe: `${m.motorista} · ${plural(m.quantidadeRoteiros, 'roteiro', 'roteiros')} · total ${formatarMinutos(m.tempoParadoMinutos)}`,
                    minutos: m.mediaPorRoteiroMinutos,
                  }))}
                  descricao="Tempo parado médio por roteiro de cada motorista"
                />
              </Card>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// Visão em tabela do gráfico principal: todo valor do gráfico fica
// acessível sem depender do tooltip.
function TabelaSerie({ serie }: { serie: ItemGrafico[] }) {
  return (
    <div className="max-h-72 overflow-y-auto">
      <table className="w-full text-sm">
        <thead className="sticky top-0 bg-white">
          <tr className="text-left text-xs text-grafite">
            <th className="py-2 font-medium">Recorte</th>
            <th className="py-2 font-medium">Detalhe</th>
            <th className="py-2 font-medium text-right">Tempo parado</th>
          </tr>
        </thead>
        <tbody>
          {serie.map((item, i) => (
            <tr key={i} className="border-t border-neutral-100">
              <td className="py-2 num">{item.rotulo}</td>
              <td className="py-2 text-grafite">{item.detalhe}</td>
              <td className="py-2 num text-right font-medium">{formatarMinutos(item.minutos)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
