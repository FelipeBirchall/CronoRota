import { Fragment, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import type { Alteracao, Roteiro } from '../types';
import { ListaAlteracoes } from '../components/ListaAlteracoes';
import { AjusteHorarioForm } from '../components/AjusteHorarioForm';
import { Alert, Card } from '../components/ui';
import { formatarHora, formatarMinutos } from '../utils/data';

export function RoteiroDetalhePage() {
  const { id } = useParams();
  const [roteiro, setRoteiro] = useState<Roteiro | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [alteracoes, setAlteracoes] = useState<Alteracao[] | null>(null);
  const [erroAlteracoes, setErroAlteracoes] = useState<string | null>(null);
  const [historicoAberto, setHistoricoAberto] = useState(false);
  const [pontoEmAjuste, setPontoEmAjuste] = useState<number | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  function carregarRoteiro() {
    if (!id) return;
    api.get<Roteiro>(`/roteiros/${id}`).then(setRoteiro).catch((e) => setErro(e.message));
  }

  // RNF05: a trilha só é buscada quando o gerente abre a seção.
  function carregarAlteracoes() {
    if (!id) return;
    api.get<Alteracao[]>(`/roteiros/${id}/alteracoes`).then(setAlteracoes).catch((e) => setErroAlteracoes(e.message));
  }

  useEffect(carregarRoteiro, [id]);

  // Depois de um ajuste: recarrega os tempos e, se o histórico estiver
  // aberto, a trilha - o ajuste acabou de entrar nela.
  function aposAjuste() {
    setPontoEmAjuste(null);
    setAviso('Horário ajustado. Tempo parado e totais do roteiro recalculados.');
    carregarRoteiro();
    if (historicoAberto) carregarAlteracoes();
    else setAlteracoes(null);
  }

  if (erro) return <Card><p className="text-sm text-terracota">{erro}</p></Card>;
  if (!roteiro) return <p className="text-sm text-grafite">Carregando...</p>;

  // O laranja de alerta (terracota) só entra aqui quando o percentual da
  // jornada realmente passa de um patamar alto - não é decoração, é sinal.
  const alerta = (roteiro.percentualJornada ?? 0) > 20;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">Roteiro #{roteiro.id}</h1>
          <p className="text-sm text-grafite mt-1">{roteiro.motorista} · {roteiro.data}</p>
        </div>
        <Link
          to={`/motorista/roteiro/${roteiro.id}`}
          className="text-sm text-petroleo font-medium hover:underline"
        >
          Ver como o motorista veria →
        </Link>
      </div>

      <div className="grid grid-cols-3 gap-4">
        <Card>
          <p className="text-xs text-grafite mb-1">Tempo total parado</p>
          <p className={`text-2xl font-semibold num ${alerta ? 'text-terracota' : 'text-[#1a1a1a]'}`}>
            {formatarMinutos(roteiro.tempoTotalParadoMinutos)}
          </p>
        </Card>
        <Card>
          <p className="text-xs text-grafite mb-1">% da jornada (8h)</p>
          <p className={`text-2xl font-semibold num ${alerta ? 'text-terracota' : 'text-verdeok'}`}>
            {roteiro.percentualJornada !== null ? `${roteiro.percentualJornada.toLocaleString('pt-BR')}%` : '—'}
          </p>
        </Card>
        <Card>
          <p className="text-xs text-grafite mb-1">Custo estimado</p>
          <p className="text-2xl font-semibold num">
            {roteiro.custoEstimado !== null
              ? roteiro.custoEstimado.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
              : '—'}
          </p>
        </Card>
      </div>

      <Card>
        <h2 className="text-sm font-medium text-grafite mb-3">Pontos do roteiro</h2>
        {aviso && <div className="mb-3"><Alert tipo="sucesso">{aviso}</Alert></div>}
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-xs text-grafite">
              <th className="pb-2 font-medium">Ordem</th>
              <th className="pb-2 font-medium">Endereço</th>
              <th className="pb-2 font-medium">Chegada</th>
              <th className="pb-2 font-medium">Saída</th>
              <th className="pb-2 font-medium">Tempo parado</th>
              <th className="pb-2" />
            </tr>
          </thead>
          <tbody>
            {roteiro.pontos.map((p) => (
              <Fragment key={p.id}>
                <tr className="border-t border-neutral-100">
                  <td className="py-2.5 num text-grafite">{p.ordem}</td>
                  <td className="py-2.5">{p.endereco}</td>
                  <td className="py-2.5 num text-grafite">{formatarHora(p.dataHoraChegada)}</td>
                  <td className="py-2.5 num text-grafite">{formatarHora(p.dataHoraSaida)}</td>
                  <td className="py-2.5 num font-medium">
                    {p.ordem === 1 ? <span className="text-grafite font-normal">partida</span> : formatarMinutos(p.tempoParadoMinutos)}
                  </td>
                  <td className="py-2.5 text-right">
                    {/* UC07-A3: corrigir um horário registrado indevidamente */}
                    <button
                      type="button"
                      onClick={() => { setAviso(null); setPontoEmAjuste(pontoEmAjuste === p.id ? null : p.id); }}
                      className="text-xs font-medium text-petroleo hover:underline"
                    >
                      Ajustar
                    </button>
                  </td>
                </tr>
                {pontoEmAjuste === p.id && (
                  <tr>
                    <td colSpan={6} className="pb-3">
                      <AjusteHorarioForm ponto={p} onSalvo={aposAjuste} onCancelar={() => setPontoEmAjuste(null)} />
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      </Card>

      <details className="bg-white rounded-lg border border-neutral-200" onToggle={(e) => {
        const aberto = e.currentTarget.open;
        setHistoricoAberto(aberto);
        if (aberto && !alteracoes) carregarAlteracoes();
      }}>
        <summary className="px-6 py-4 text-sm font-medium text-grafite cursor-pointer select-none">
          Histórico de alterações do roteiro e dos pontos
        </summary>
        <div className="px-6 pb-4">
          {erroAlteracoes && <p className="text-sm text-terracota">{erroAlteracoes}</p>}
          {!alteracoes && !erroAlteracoes && <p className="text-sm text-grafite">Carregando...</p>}
          {alteracoes && <ListaAlteracoes alteracoes={alteracoes} />}
        </div>
      </details>
    </div>
  );
}
