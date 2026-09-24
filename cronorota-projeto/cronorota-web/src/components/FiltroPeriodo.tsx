import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import type { Motorista } from '../types';
import { hojeLocal, passaDe12Meses, primeiroDiaDoMes, somarDias } from '../utils/data';

export interface Periodo {
  inicio: string;
  fim: string;
  motoristaId: string; // '' = todos
}

// O período vive na URL (?inicio=...&fim=...&motoristaId=...): recarregar a
// página mantém o filtro, e o dashboard pode mandar para o histórico de um
// dia específico só montando o link (drill-down, UC10-A1).
export function usePeriodo(diasPadrao = 30): [Periodo, (p: Periodo) => void] {
  const [params, setParams] = useSearchParams();
  const hoje = hojeLocal();
  const periodo = {
    inicio: params.get('inicio') ?? somarDias(hoje, -(diasPadrao - 1)),
    fim: params.get('fim') ?? hoje,
    motoristaId: params.get('motoristaId') ?? '',
  };
  function definir(p: Periodo) {
    const novos: Record<string, string> = { inicio: p.inicio, fim: p.fim };
    if (p.motoristaId) novos.motoristaId = p.motoristaId;
    setParams(novos, { replace: true });
  }
  return [periodo, definir];
}

export function queryDoPeriodo(p: Periodo): string {
  const q = new URLSearchParams({ inicio: p.inicio, fim: p.fim });
  if (p.motoristaId) q.set('motoristaId', p.motoristaId);
  return q.toString();
}

function atalhos(hoje: string) {
  return [
    { rotulo: 'Hoje', inicio: hoje, fim: hoje },
    { rotulo: '7 dias', inicio: somarDias(hoje, -6), fim: hoje },
    { rotulo: '30 dias', inicio: somarDias(hoje, -29), fim: hoje },
    { rotulo: 'Este mês', inicio: primeiroDiaDoMes(hoje), fim: hoje },
    { rotulo: '12 meses', inicio: somarDias(hoje, -364), fim: hoje },
  ];
}

// Uma linha de filtros acima de tudo o que eles afetam: atalhos de período
// primeiro, depois o intervalo livre e, para o gerente, o motorista.
export function FiltroPeriodo({
  periodo,
  onChange,
  mostrarMotorista,
}: {
  periodo: Periodo;
  onChange: (p: Periodo) => void;
  mostrarMotorista: boolean;
}) {
  const [motoristas, setMotoristas] = useState<Motorista[]>([]);
  const [inicio, setInicio] = useState(periodo.inicio);
  const [fim, setFim] = useState(periodo.fim);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    setInicio(periodo.inicio);
    setFim(periodo.fim);
  }, [periodo.inicio, periodo.fim]);

  useEffect(() => {
    if (mostrarMotorista) api.get<Motorista[]>('/motoristas').then(setMotoristas).catch(() => {});
  }, [mostrarMotorista]);

  function aplicar(novo: Periodo) {
    setErro(null);
    // UC09-E1: data inicial posterior à final.
    if (novo.inicio > novo.fim) {
      setErro('A data inicial precisa ser anterior ou igual à final.');
      return;
    }
    // UC09-E3: acima de 12 meses, alerta sobre o volume e pede confirmação.
    if (passaDe12Meses(novo.inicio, novo.fim)
      && !window.confirm('O período passa de 12 meses e pode demorar para carregar. Continuar?')) {
      return;
    }
    onChange(novo);
  }

  const hoje = hojeLocal();

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-end gap-3">
        <div className="flex w-full sm:w-auto rounded-md border border-neutral-300 bg-white overflow-hidden">
          {atalhos(hoje).map((a) => {
            const ativo = a.inicio === periodo.inicio && a.fim === periodo.fim;
            return (
              <button
                key={a.rotulo}
                type="button"
                onClick={() => aplicar({ ...periodo, inicio: a.inicio, fim: a.fim })}
                className={`flex-1 sm:flex-none whitespace-nowrap px-2.5 sm:px-3 py-2 text-sm border-r border-neutral-200 last:border-r-0 transition-colors ${
                  ativo ? 'bg-petroleo text-white font-medium' : 'text-grafite hover:bg-neutral-50'
                }`}
              >
                {a.rotulo}
              </button>
            );
          })}
        </div>

        <form
          className="flex flex-wrap items-end gap-2 w-full sm:w-auto"
          onSubmit={(e) => {
            e.preventDefault();
            aplicar({ ...periodo, inicio, fim });
          }}
        >
          <label className="text-xs text-grafite flex-1 min-w-[8.5rem] sm:flex-none">
            De
            <input type="date" required value={inicio} onChange={(e) => setInicio(e.target.value)}
              className="block w-full mt-1 rounded-md border border-neutral-300 px-2 py-1.5 text-sm" />
          </label>
          <label className="text-xs text-grafite flex-1 min-w-[8.5rem] sm:flex-none">
            Até
            <input type="date" required value={fim} onChange={(e) => setFim(e.target.value)}
              className="block w-full mt-1 rounded-md border border-neutral-300 px-2 py-1.5 text-sm" />
          </label>
          <button type="submit" className="px-3 py-2 rounded-md text-sm font-medium text-petroleo border border-petroleo hover:bg-petroleo/5">
            Aplicar
          </button>
        </form>

        {mostrarMotorista && (
          <label className="text-xs text-grafite">
            Motorista
            <select
              value={periodo.motoristaId}
              onChange={(e) => onChange({ ...periodo, motoristaId: e.target.value })}
              className="block mt-1 rounded-md border border-neutral-300 px-2 py-1.5 text-sm bg-white"
            >
              <option value="">Toda a equipe</option>
              {motoristas.map((m) => <option key={m.id} value={m.id}>{m.nome}</option>)}
            </select>
          </label>
        )}
      </div>
      {erro && <p className="text-sm text-terracota">{erro}</p>}
    </div>
  );
}
