import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import type { Gerente, Motorista, Pedido, Roteiro } from '../types';
import { Alert, Button, Card, Field, Input, Select } from '../components/ui';

// A ordem em que o gerente marca os pedidos abaixo VIRA a ordem dos pontos
// no roteiro (RN06) - o primeiro marcado é o ponto de partida (RN01) e não
// acumula tempo parado, então a ordem de clique importa de verdade aqui,
// não é só estética.
export function MontarRoteiroPage() {
  const navigate = useNavigate();
  const [motoristas, setMotoristas] = useState<Motorista[]>([]);
  const [gerentes, setGerentes] = useState<Gerente[]>([]);
  const [pedidosPendentes, setPedidosPendentes] = useState<Pedido[]>([]);
  const [motoristaId, setMotoristaId] = useState('');
  const [gerenteId, setGerenteId] = useState('');
  const [data, setData] = useState(() => new Date().toISOString().slice(0, 10));
  const [distanciaTotalKm, setDistanciaTotalKm] = useState('');
  const [ordemSelecionada, setOrdemSelecionada] = useState<number[]>([]); // pedido ids, na ordem de clique
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  useEffect(() => {
    api.get<Motorista[]>('/motoristas').then(setMotoristas).catch(() => {});
    api.get<Gerente[]>('/gerentes').then(setGerentes).catch(() => {});
    api.get<Pedido[]>('/pedidos/pendentes').then(setPedidosPendentes).catch(() => {});
  }, []);

  function alternarPedido(id: number) {
    setOrdemSelecionada((atual) =>
      atual.includes(id) ? atual.filter((p) => p !== id) : [...atual, id]
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    if (ordemSelecionada.length < 2) {
      setErro('Selecione pelo menos 2 pedidos (o primeiro clicado vira o ponto de partida)');
      return;
    }
    setCarregando(true);
    try {
      const roteiro = await api.post<Roteiro>('/roteiros', {
        motoristaId: Number(motoristaId),
        gerenteId: Number(gerenteId),
        data,
        pedidoIds: ordemSelecionada,
        distanciaTotalKm: Number(distanciaTotalKm),
      });
      navigate(`/gerente/roteiros/${roteiro.id}`);
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao montar roteiro');
    } finally {
      setCarregando(false);
    }
  }

  if (motoristas.length === 0 || gerentes.length === 0) {
    return <Card><p className="text-sm text-grafite">Cadastre ao menos um gerente e um motorista antes de montar um roteiro.</p></Card>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold">Montar roteiro</h1>
        <p className="text-sm text-grafite mt-1">
          Clique nos pedidos na ordem do trajeto. O primeiro clicado é o ponto de partida
          (não acumula tempo parado - RN01).
        </p>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-5">
          {erro && <Alert tipo="erro">{erro}</Alert>}

          <div className="grid grid-cols-2 gap-4">
            <Field label="Motorista">
              <Select required value={motoristaId} onChange={(e) => setMotoristaId(e.target.value)}>
                <option value="">Selecione...</option>
                {motoristas.map((m) => <option key={m.id} value={m.id}>{m.nome}</option>)}
              </Select>
            </Field>
            <Field label="Gerente responsável">
              <Select required value={gerenteId} onChange={(e) => setGerenteId(e.target.value)}>
                <option value="">Selecione...</option>
                {gerentes.map((g) => <option key={g.id} value={g.id}>{g.nome}</option>)}
              </Select>
            </Field>
            <Field label="Data">
              <Input required type="date" value={data} onChange={(e) => setData(e.target.value)} />
            </Field>
            <Field label="Distância total estimada (km)">
              <Input required type="number" step="0.1" min="0.1" value={distanciaTotalKm}
                onChange={(e) => setDistanciaTotalKm(e.target.value)} placeholder="ex.: 48" />
            </Field>
          </div>

          <div>
            <p className="text-sm font-medium text-grafite mb-2">Pedidos pendentes</p>
            {pedidosPendentes.length === 0 && (
              <p className="text-sm text-grafite">Nenhum pedido pendente - cadastre pedidos na aba "Pedidos".</p>
            )}
            <div className="space-y-2">
              {pedidosPendentes.map((p) => {
                const posicao = ordemSelecionada.indexOf(p.id);
                const selecionado = posicao !== -1;
                return (
                  <button
                    type="button"
                    key={p.id}
                    onClick={() => alternarPedido(p.id)}
                    className={`w-full flex items-center gap-3 text-left px-4 py-3 rounded-md border text-sm transition-colors ${
                      selecionado
                        ? 'border-petroleo bg-petroleo/5'
                        : 'border-neutral-200 hover:border-neutral-300'
                    }`}
                  >
                    <span
                      className={`shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-xs font-semibold num ${
                        selecionado ? 'bg-petroleo text-white' : 'bg-neutral-100 text-grafite'
                      }`}
                    >
                      {selecionado ? posicao + 1 : ''}
                    </span>
                    <span className="flex-1">
                      <span className="font-medium">{p.codigo}</span>
                      <span className="text-grafite"> · {p.destinatario} · {p.endereco.logradouro}</span>
                    </span>
                    {selecionado && posicao === 0 && (
                      <span className="text-xs text-petroleo font-medium">Partida</span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          <Button type="submit" disabled={carregando}>{carregando ? 'Montando...' : 'Montar roteiro'}</Button>
        </form>
      </Card>
    </div>
  );
}
