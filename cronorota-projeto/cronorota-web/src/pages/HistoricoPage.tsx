import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { FiltroPeriodo, queryDoPeriodo, usePeriodo } from '../components/FiltroPeriodo';
import { Card } from '../components/ui';
import type { Historico, LinhaHistorico } from '../types';
import { formatarData, formatarHora, formatarMinutos } from '../utils/data';

type Ordenacao = 'cronologica' | 'maiorTempo';

// UC09 - Consultar histórico de tempos parados. O mesmo conteúdo serve ao
// gerente (dentro do painel) e ao motorista (tela móvel própria, abaixo);
// o que cada um enxerga (RN13) é filtrado no back-end.
function ConteudoHistorico({ linkRoteiro }: { linkRoteiro: (id: number) => string }) {
  const { sessao } = useAuth();
  const [periodo, setPeriodo] = usePeriodo(30);
  const [historico, setHistorico] = useState<Historico | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [ordenacao, setOrdenacao] = useState<Ordenacao>('cronologica');
  const [exportando, setExportando] = useState<'csv' | 'pdf' | null>(null);
  const [erroExportacao, setErroExportacao] = useState<string | null>(null);

  // UC14: exporta o mesmo recorte e a mesma ordenação que estão na tela.
  // Se falhar (E1 volume, E2 geração), a consulta continua em tela e só
  // aparece a mensagem.
  async function exportar(formato: 'csv' | 'pdf') {
    setErroExportacao(null);
    setExportando(formato);
    try {
      const query = `${queryDoPeriodo(periodo)}&formato=${formato}&maiorTempo=${ordenacao === 'maiorTempo'}`;
      await api.baixar(`/historico/exportar?${query}`, `cronorota-historico.${formato}`);
    } catch (e) {
      setErroExportacao(e instanceof Error ? e.message : 'Erro ao exportar');
    } finally {
      setExportando(null);
    }
  }

  useEffect(() => {
    setErro(null);
    setCarregando(true);
    api.get<Historico>(`/historico?${queryDoPeriodo(periodo)}`)
      .then(setHistorico)
      .catch((e) => setErro(e.message))
      .finally(() => setCarregando(false));
  }, [periodo.inicio, periodo.fim, periodo.motoristaId]);

  // UC09-A3: ordenar por tempo parado para achar os maiores gargalos.
  const linhas: LinhaHistorico[] = historico
    ? ordenacao === 'maiorTempo'
      ? [...historico.pontos].sort((a, b) => b.tempoParadoMinutos - a.tempoParadoMinutos)
      : historico.pontos
    : [];
  const mostrarMotorista = sessao?.perfil !== 'MOTORISTA';

  return (
    <div className="space-y-5">
      <FiltroPeriodo periodo={periodo} onChange={setPeriodo} mostrarMotorista={mostrarMotorista} />

      {erro && <Card><p className="text-sm text-terracota">{erro}</p></Card>}

      {historico && (
        <div className={`space-y-5 transition-opacity ${carregando ? 'opacity-50' : ''}`}>
          {/* UC09 passo 6: totalizadores do período */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <Totalizador rotulo="Tempo total parado" valor={formatarMinutos(historico.tempoTotalParadoMinutos)} />
            <Totalizador rotulo="Média por roteiro" valor={formatarMinutos(historico.mediaPorRoteiroMinutos)} />
            <Totalizador rotulo="Média por ponto" valor={formatarMinutos(historico.mediaPorPontoMinutos)} />
            <Totalizador rotulo="Roteiros · paradas" valor={`${historico.quantidadeRoteiros} · ${historico.quantidadePontos}`} />
          </div>

          <Card className="!p-0">
            <div className="flex flex-wrap items-center justify-between gap-3 px-5 py-3 border-b border-neutral-100">
              <h2 className="text-sm font-medium text-grafite">Paradas no período</h2>
              <div className="flex flex-wrap items-center gap-3">
                <label className="text-xs text-grafite flex items-center gap-2">
                  Ordenar
                  <select value={ordenacao} onChange={(e) => setOrdenacao(e.target.value as Ordenacao)}
                    className="rounded-md border border-neutral-300 px-2 py-1 text-xs bg-white">
                    <option value="cronologica">Por data</option>
                    <option value="maiorTempo">Maior tempo parado</option>
                  </select>
                </label>
                {/* UC09-A1 / UC14: exportar o que está em tela */}
                <div className="flex items-center gap-1.5">
                  <span className="text-xs text-grafite">Exportar</span>
                  {(['csv', 'pdf'] as const).map((formato) => (
                    <button
                      key={formato}
                      type="button"
                      onClick={() => exportar(formato)}
                      disabled={exportando !== null || linhas.length === 0}
                      className="px-2.5 py-1 rounded-md border border-petroleo text-xs font-medium text-petroleo hover:bg-petroleo/5 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      {exportando === formato ? 'Gerando...' : formato.toUpperCase()}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            {erroExportacao && (
              <p className="px-5 py-2 text-sm text-terracota border-b border-neutral-100">{erroExportacao}</p>
            )}

            {linhas.length === 0 ? (
              // UC09-E2
              <p className="px-5 py-6 text-sm text-grafite">Nenhum roteiro encontrado para o período informado.</p>
            ) : (
              <>
                {/* Desktop: tabela. Celular (motorista em campo): lista em cartões. */}
                <table className="hidden md:table w-full text-sm">
                  <thead>
                    <tr className="text-left text-xs text-grafite">
                      <th className="px-5 py-2 font-medium">Data</th>
                      {mostrarMotorista && <th className="py-2 font-medium">Motorista</th>}
                      <th className="py-2 font-medium">Ponto</th>
                      <th className="py-2 font-medium">Endereço</th>
                      <th className="py-2 font-medium">Chegada</th>
                      <th className="py-2 font-medium">Saída</th>
                      <th className="px-5 py-2 font-medium text-right">Tempo parado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {linhas.map((l) => (
                      <tr key={`${l.roteiroId}-${l.ordem}`} className="border-t border-neutral-100">
                        <td className="px-5 py-2.5 num">
                          {/* UC09-A2: detalhamento do roteiro do dia */}
                          <Link to={linkRoteiro(l.roteiroId)} className="text-petroleo hover:underline">{formatarData(l.data)}</Link>
                        </td>
                        {mostrarMotorista && <td className="py-2.5">{l.motorista}</td>}
                        <td className="py-2.5 num text-grafite">{l.ordem}</td>
                        <td className="py-2.5">{l.endereco}</td>
                        <td className="py-2.5 num text-grafite">{formatarHora(l.dataHoraChegada)}</td>
                        <td className="py-2.5 num text-grafite">{formatarHora(l.dataHoraSaida)}</td>
                        <td className="px-5 py-2.5 num font-medium text-right">{formatarMinutos(l.tempoParadoMinutos)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                <ul className="md:hidden divide-y divide-neutral-100">
                  {linhas.map((l) => (
                    <li key={`${l.roteiroId}-${l.ordem}`}>
                      <Link to={linkRoteiro(l.roteiroId)} className="flex items-start justify-between gap-3 px-4 py-3">
                        <div>
                          <p className="text-xs text-grafite num">{formatarData(l.data)} · ponto {l.ordem}</p>
                          <p className="text-sm font-medium leading-snug">{l.endereco}</p>
                          <p className="text-xs text-grafite num mt-0.5">
                            {formatarHora(l.dataHoraChegada)} – {formatarHora(l.dataHoraSaida)}
                          </p>
                        </div>
                        <span className="shrink-0 text-sm font-semibold num">{formatarMinutos(l.tempoParadoMinutos)}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </Card>
        </div>
      )}
    </div>
  );
}

function Totalizador({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div className="bg-white rounded-lg border border-neutral-200 px-4 py-3">
      <p className="text-xs text-grafite">{rotulo}</p>
      <p className="text-lg font-semibold mt-0.5">{valor}</p>
    </div>
  );
}

// Gerente: dentro do LayoutGerente.
export function HistoricoPage() {
  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-xl font-semibold">Histórico</h1>
        <p className="text-sm text-grafite mt-1">Pontos e tempos parados da sua equipe no período (UC09).</p>
      </div>
      <ConteudoHistorico linkRoteiro={(id) => `/gerente/roteiros/${id}`} />
    </div>
  );
}

// Motorista: tela móvel, no mesmo estilo de "Meus roteiros".
export function HistoricoMotoristaPage() {
  return (
    <div className="min-h-screen bg-fundo">
      <header className="bg-petroleo text-white px-5 py-4">
        <Link to="/motorista/meus-roteiros" className="text-xs text-white/70 hover:text-white">← meus roteiros</Link>
        <h1 className="text-lg font-semibold mt-0.5">Meu histórico</h1>
      </header>
      <div className="p-4">
        <ConteudoHistorico linkRoteiro={(id) => `/motorista/roteiro/${id}`} />
      </div>
    </div>
  );
}
