import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { Roteiro } from '../types';

// Home do motorista depois do login (RN13: só os roteiros dele mesmo,
// filtrado no back-end pelo id que vem do token, não por um parâmetro que
// o front-end poderia manipular).
export function MeusRoteirosPage() {
  const { sessao, logout } = useAuth();
  const [roteiros, setRoteiros] = useState<Roteiro[]>([]);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    api.get<Roteiro[]>('/roteiros/meus').then(setRoteiros).catch((e) => setErro(e.message));
  }, []);

  return (
    <div className="min-h-screen bg-fundo">
      <header className="bg-petroleo text-white px-5 py-4 flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold">Olá, {sessao?.nome}</h1>
          <p className="text-sm text-white/80">Seus roteiros</p>
        </div>
        <div className="flex items-center gap-4">
          <Link to="/motorista/historico" className="text-xs text-white/90 hover:text-white">Histórico</Link>
          <button onClick={logout} className="text-xs text-white/70 hover:text-white">Sair</button>
        </div>
      </header>

      <div className="p-4 space-y-3">
        {erro && <p className="text-sm text-terracota">{erro}</p>}
        {roteiros.length === 0 && !erro && (
          <p className="text-sm text-grafite">Nenhum roteiro atribuído a você ainda.</p>
        )}
        {roteiros.map((r) => (
          <Link
            key={r.id}
            to={`/motorista/roteiro/${r.id}`}
            className="block bg-white rounded-xl border border-neutral-200 p-4"
          >
            <p className="text-xs text-grafite">{r.data}</p>
            <p className="font-medium mt-0.5">{r.pontos.length} pontos no roteiro</p>
            {r.tempoTotalParadoMinutos !== null && (
              <p className="text-sm text-grafite mt-1 num">{r.tempoTotalParadoMinutos} min parado até agora</p>
            )}
          </Link>
        ))}
      </div>
    </div>
  );
}
