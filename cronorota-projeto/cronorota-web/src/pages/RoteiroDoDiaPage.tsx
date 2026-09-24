import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { Ponto, Roteiro } from '../types';

// Esta é a tela "Roteiro do dia (móvel)" da seção 16 do documento: lista
// sequencial de pontos, um botão de chegada/saída por vez, pensada pra ser
// usada com uma mão só, ao ar livre, sob luz de sol - por isso o contraste
// alto e os alvos de toque grandes, diferente da densidade da visão do
// gerente.
export function RoteiroDoDiaPage() {
  const { id } = useParams();
  const { sessao, logout } = useAuth();
  const [roteiro, setRoteiro] = useState<Roteiro | null>(null);
  const [carregandoPontoId, setCarregandoPontoId] = useState<number | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  function carregar() {
    if (!id) return;
    api.get<Roteiro>(`/roteiros/${id}`).then(setRoteiro).catch((e) => setErro(e.message));
  }

  useEffect(carregar, [id]);

  // Em uso real, o botão sempre registraria o instante em que foi tocado -
  // não faria sentido o motorista escolher a própria hora. Mas pra TESTAR
  // regras como parada que atravessa a meia-noite (UC08-E1) sem esperar o
  // relógio de verdade passar, esse override deixa simular qualquer
  // horário. Em produção, esse campo (e o modoTeste) seria removido.
  const [modoTeste, setModoTeste] = useState(false);
  const [horarioSimulado, setHorarioSimulado] = useState('');

  async function registrar(ponto: Ponto, acao: 'chegada' | 'saida') {
    setErro(null);
    setCarregandoPontoId(ponto.id);
    try {
      const dataHora = modoTeste && horarioSimulado
        ? new Date(horarioSimulado).toISOString()
        : new Date().toISOString();
      await api.post(`/pontos/${ponto.id}/${acao}`, { dataHora });
      carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao registrar');
    } finally {
      setCarregandoPontoId(null);
    }
  }

  const proximoPendenteId = roteiro?.pontos.find((p) => !p.dataHoraSaida)?.id;

  if (erro && !roteiro) {
    return <div className="min-h-screen bg-petroleo flex items-center justify-center p-6"><p className="text-white text-sm">{erro}</p></div>;
  }
  if (!roteiro) {
    return <div className="min-h-screen bg-petroleo flex items-center justify-center"><p className="text-white text-sm">Carregando...</p></div>;
  }

  return (
    <div className="min-h-screen bg-fundo">
      <header className="bg-petroleo text-white px-5 py-4 sticky top-0 flex items-start justify-between">
        <div>
          {/* O link de volta muda com quem está olhando: o gerente
              veio da tela de detalhe pra "espiar" essa visão; o motorista
              de verdade não tem uma "visão do gerente" pra voltar. */}
          {sessao?.perfil === 'GERENTE' ? (
            <Link to={`/gerente/roteiros/${roteiro.id}`} className="text-xs text-white/70 hover:text-white">← visão do gerente</Link>
          ) : (
            <Link to="/motorista/meus-roteiros" className="text-xs text-white/70 hover:text-white">← meus roteiros</Link>
          )}
          <h1 className="text-lg font-semibold mt-0.5">Roteiro de hoje</h1>
          <p className="text-sm text-white/80">{roteiro.data}</p>
        </div>
        {sessao?.perfil === 'MOTORISTA' && (
          <button onClick={logout} className="text-xs text-white/70 hover:text-white">Sair</button>
        )}
      </header>

      {erro && (
        <div className="mx-4 mt-4 bg-terracota/10 border border-terracota/30 text-terracota text-sm rounded-md px-4 py-3">
          {erro}
        </div>
      )}

      <div className="mx-4 mt-4 bg-white border border-dashed border-neutral-300 rounded-lg p-3">
        <label className="flex items-center gap-2 text-xs text-grafite">
          <input type="checkbox" checked={modoTeste} onChange={(e) => setModoTeste(e.target.checked)} />
          Modo teste: simular horário do registro (em vez de usar o agora)
        </label>
        {modoTeste && (
          <input
            type="datetime-local"
            value={horarioSimulado}
            onChange={(e) => setHorarioSimulado(e.target.value)}
            className="mt-2 w-full rounded-md border border-neutral-300 px-3 py-2 text-sm"
          />
        )}
      </div>

      <div className="p-4 space-y-3">
        {roteiro.pontos.map((ponto) => {
          // UC07-A1: o ponto de partida só tem saída - o motorista já está
          // lá quando o roteiro começa, então não existe "chegada" nele.
          const partida = ponto.ordem === 1;
          const emAtendimento = !partida && ponto.dataHoraChegada && !ponto.dataHoraSaida;
          const concluido = !!ponto.dataHoraSaida;
          const carregando = carregandoPontoId === ponto.id;
          // UC07 passo 2: destaca o próximo ponto pendente da sequência.
          const proximo = ponto.id === proximoPendenteId;

          return (
            <div
              key={ponto.id}
              className={`bg-white rounded-xl border p-4 ${proximo ? 'border-petroleo ring-1 ring-petroleo' : 'border-neutral-200'}`}
            >
              <div className="flex items-start justify-between gap-3 mb-3">
                <div>
                  <p className="text-xs text-grafite mb-0.5">
                    Ponto {ponto.ordem} {ponto.ordem === 1 && '· partida'}
                  </p>
                  <p className="font-medium leading-snug">{ponto.endereco}</p>
                </div>
                {concluido && (
                  <span className="shrink-0 text-xs font-medium bg-verdeok/10 text-verdeok px-2.5 py-1 rounded-full">
                    {partida ? 'saída registrada' : 'concluído'}
                  </span>
                )}
                {proximo && !emAtendimento && (
                  <span className="shrink-0 text-xs font-medium bg-petroleo/10 text-petroleo px-2.5 py-1 rounded-full">
                    próximo
                  </span>
                )}
                {emAtendimento && (
                  <span className="shrink-0 text-xs font-medium bg-terracota/10 text-terracota px-2.5 py-1 rounded-full">
                    em atendimento
                  </span>
                )}
              </div>

              {concluido && !partida && (
                <p className="text-sm text-grafite mb-3 num">
                  Tempo parado: <span className="font-semibold text-[#1a1a1a]">{ponto.tempoParadoMinutos} min</span>
                </p>
              )}

              {partida && !concluido && (
                <button
                  onClick={() => registrar(ponto, 'saida')}
                  disabled={carregando}
                  className="w-full py-3.5 rounded-lg bg-petroleo text-white font-medium text-base disabled:opacity-50"
                >
                  {carregando ? 'Registrando...' : 'Registrar saída da partida'}
                </button>
              )}
              {!partida && !ponto.dataHoraChegada && (
                <button
                  onClick={() => registrar(ponto, 'chegada')}
                  disabled={carregando}
                  className="w-full py-3.5 rounded-lg bg-petroleo text-white font-medium text-base disabled:opacity-50"
                >
                  {carregando ? 'Registrando...' : 'Registrar chegada'}
                </button>
              )}
              {emAtendimento && (
                <button
                  onClick={() => registrar(ponto, 'saida')}
                  disabled={carregando}
                  className="w-full py-3.5 rounded-lg bg-terracota text-white font-medium text-base disabled:opacity-50"
                >
                  {carregando ? 'Registrando...' : 'Registrar saída'}
                </button>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
