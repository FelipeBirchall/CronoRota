import type { Alteracao } from '../types';
import { formatarDataHora } from '../utils/data';

const OPERACAO: Record<Alteracao['operacao'], { rotulo: string; classe: string }> = {
  INCLUSAO: { rotulo: 'Inclusão', classe: 'bg-verdeok/10 text-verdeok' },
  ALTERACAO: { rotulo: 'Alteração', classe: 'bg-petroleo/10 text-petroleo' },
  REMOCAO: { rotulo: 'Remoção', classe: 'bg-terracota/10 text-terracota' },
};

const PERFIL: Record<string, string> = {
  MOTORISTA: 'motorista',
  GERENTE: 'gerente',
  ADMINISTRADOR: 'administrador',
  SISTEMA: 'sistema',
};

// "migracao-v3" é a revisão que registrou, na criação da auditoria, o que
// já existia no banco - não foi uma pessoa que fez aquela inclusão.
function autor(a: Alteracao): string {
  if (a.login === 'migracao-v3') return 'Estado inicial (criação da auditoria)';
  return `${a.login} · ${PERFIL[a.perfil] ?? a.perfil}`;
}

// Trilha de auditoria (RNF05): quem, quando, o quê e, campo a campo, o
// valor anterior e o novo - o RegistroAuditoria da seção 13 do documento.
export function ListaAlteracoes({ alteracoes }: { alteracoes: Alteracao[] }) {
  if (alteracoes.length === 0) {
    return <p className="text-sm text-grafite py-4">Nenhuma alteração registrada.</p>;
  }

  return (
    <ul className="divide-y divide-neutral-100">
      {alteracoes.map((a) => {
        const op = OPERACAO[a.operacao];
        return (
          <li key={`${a.revisao}-${a.entidade}-${a.registroId}`} className="py-3">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
              <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${op.classe}`}>{op.rotulo}</span>
              <span className="font-medium">{a.entidadeNome} #{a.registroId}</span>
              <span className="text-grafite num">{formatarDataHora(a.instante)}</span>
              <span className="text-grafite">{autor(a)}</span>
              <span className="text-xs text-grafite/70 num ml-auto">revisão {a.revisao}</span>
            </div>
            {/* UC07-A3: o motivo do ajuste manual */}
            {a.justificativa && (
              <p className="mt-1.5 text-sm">
                <span className="text-grafite">Justificativa: </span>
                <span className="italic">“{a.justificativa}”</span>
              </p>
            )}
            {a.campos.length > 0 && (
              <table className="mt-2 text-sm w-full max-w-2xl">
                <tbody>
                  {a.campos.map((c) => (
                    <tr key={c.campo}>
                      <td className="py-0.5 pr-4 text-grafite w-48 align-top">{c.campo}</td>
                      <td className="py-0.5 num">
                        {a.operacao !== 'INCLUSAO' && (
                          <>
                            <span className="text-grafite line-through decoration-grafite/40">{c.anterior ?? 'vazio'}</span>
                            <span className="text-grafite mx-2" aria-label="passou para">→</span>
                          </>
                        )}
                        <span>{a.operacao === 'REMOCAO' ? '' : c.novo ?? 'vazio'}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </li>
        );
      })}
    </ul>
  );
}
