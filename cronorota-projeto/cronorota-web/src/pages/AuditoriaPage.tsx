import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { FiltroPeriodo, usePeriodo } from '../components/FiltroPeriodo';
import { ListaAlteracoes } from '../components/ListaAlteracoes';
import { Card } from '../components/ui';
import type { Alteracao, RegistroExportacao } from '../types';
import { formatarData, formatarDataHora } from '../utils/data';

type Aba = 'alteracoes' | 'exportacoes';

const ENTIDADES = [
  { valor: '', rotulo: 'Todas as entidades' },
  { valor: 'PONTO', rotulo: 'Pontos' },
  { valor: 'ROTEIRO', rotulo: 'Roteiros' },
  { valor: 'PARAMETRO', rotulo: 'Parâmetros' },
  { valor: 'PEDIDO', rotulo: 'Pedidos' },
  { valor: 'MOTORISTA', rotulo: 'Motoristas' },
  { valor: 'GERENTE', rotulo: 'Gerentes' },
  { valor: 'ADMINISTRADOR', rotulo: 'Administradores' },
  { valor: 'VEICULO', rotulo: 'Veículos' },
  { valor: 'ENDERECO', rotulo: 'Endereços' },
];

// Mesmo teto do back-end (AuditoriaService.LIMITE_PADRAO).
const LIMITE = 200;

// RNF05 - trilha de auditoria, só para o administrador.
export function AuditoriaPage() {
  const [periodo, setPeriodo] = usePeriodo(7);
  const [aba, setAba] = useState<Aba>('alteracoes');
  const [entidade, setEntidade] = useState('');
  const [alteracoes, setAlteracoes] = useState<Alteracao[] | null>(null);
  const [exportacoes, setExportacoes] = useState<RegistroExportacao[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    setErro(null);
    const q = `inicio=${periodo.inicio}&fim=${periodo.fim}`;
    if (aba === 'alteracoes') {
      api.get<Alteracao[]>(`/auditoria/alteracoes?${q}${entidade ? `&entidade=${entidade}` : ''}`)
        .then(setAlteracoes).catch((e) => setErro(e.message));
    } else {
      api.get<RegistroExportacao[]>(`/auditoria/exportacoes?${q}`)
        .then(setExportacoes).catch((e) => setErro(e.message));
    }
  }, [aba, entidade, periodo.inicio, periodo.fim]);

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-xl font-semibold">Auditoria</h1>
        <p className="text-sm text-grafite mt-1">
          Quem alterou o quê e quando (RNF05). A trilha é somente para inclusão: nada aqui pode ser editado ou apagado.
        </p>
      </div>

      <FiltroPeriodo periodo={periodo} onChange={setPeriodo} mostrarMotorista={false} />

      <div className="flex flex-wrap items-center gap-3">
        <div className="flex rounded-md border border-neutral-300 overflow-hidden bg-white" role="tablist">
          {(['alteracoes', 'exportacoes'] as const).map((a) => (
            <button key={a} role="tab" aria-selected={aba === a} onClick={() => setAba(a)}
              className={`px-3 py-1.5 text-sm border-r border-neutral-200 last:border-r-0 ${
                aba === a ? 'bg-petroleo text-white font-medium' : 'text-grafite hover:bg-neutral-50'
              }`}>
              {a === 'alteracoes' ? 'Alterações' : 'Exportações de relatório'}
            </button>
          ))}
        </div>
        {aba === 'alteracoes' && (
          <select value={entidade} onChange={(e) => setEntidade(e.target.value)}
            className="rounded-md border border-neutral-300 px-2 py-1.5 text-sm bg-white">
            {ENTIDADES.map((e) => <option key={e.valor} value={e.valor}>{e.rotulo}</option>)}
          </select>
        )}
      </div>

      {erro && <Card><p className="text-sm text-terracota">{erro}</p></Card>}

      {aba === 'alteracoes' && alteracoes && (
        <Card>
          {alteracoes.length >= LIMITE && (
            <p className="text-xs text-grafite mb-2">
              Mostrando as {LIMITE} alterações mais recentes do período - reduza o período ou filtre por entidade para ver as demais.
            </p>
          )}
          <ListaAlteracoes alteracoes={alteracoes} />
        </Card>
      )}

      {aba === 'exportacoes' && exportacoes && (
        <Card className="!p-0">
          {exportacoes.length === 0 ? (
            <p className="px-5 py-6 text-sm text-grafite">Nenhuma exportação no período.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-grafite">
                  <th className="px-5 py-2 font-medium">Quando</th>
                  <th className="py-2 font-medium">Usuário</th>
                  <th className="py-2 font-medium">Formato</th>
                  <th className="py-2 font-medium">Período exportado</th>
                  <th className="py-2 font-medium">Motorista</th>
                  <th className="px-5 py-2 font-medium text-right">Paradas</th>
                </tr>
              </thead>
              <tbody>
                {exportacoes.map((r) => (
                  <tr key={r.id} className="border-t border-neutral-100">
                    <td className="px-5 py-2.5 num">{formatarDataHora(r.instante)}</td>
                    <td className="py-2.5">{r.login} <span className="text-grafite">· {r.perfil.toLowerCase()}</span></td>
                    <td className="py-2.5">{r.formato}</td>
                    <td className="py-2.5 num">{formatarData(r.periodoInicio)} a {formatarData(r.periodoFim)}</td>
                    <td className="py-2.5 text-grafite">{r.motoristaId ? `#${r.motoristaId}` : 'todos'}</td>
                    <td className="px-5 py-2.5 num text-right">{r.quantidadeLinhas}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Card>
      )}
    </div>
  );
}
