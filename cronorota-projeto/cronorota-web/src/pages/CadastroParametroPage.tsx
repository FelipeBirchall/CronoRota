import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Parametro } from '../types';
import { Alert, Button, Card, Field, Input } from '../components/ui';

const vazio = { valorCombustivel: '', jornadaPadraoMinutos: '480', dataInicioVigencia: new Date().toISOString().slice(0, 10) };

// Sem um parâmetro vigente cadastrado, o custo estimado do roteiro nunca é
// calculado (o service de custo depende de um Parametro vigente na data do
// roteiro) - por isso essa tela precisa ser preenchida antes de montar um
// roteiro, se você quiser ver o custo aparecer.
export function CadastroParametroPage() {
  const [parametros, setParametros] = useState<Parametro[]>([]);
  const [form, setForm] = useState(vazio);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  function carregar() {
    api.get<Parametro[]>('/parametros').then(setParametros).catch(() => {});
  }
  useEffect(carregar, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      await api.post('/parametros', {
        valorCombustivel: Number(form.valorCombustivel),
        jornadaPadraoMinutos: Number(form.jornadaPadraoMinutos),
        dataInicioVigencia: form.dataInicioVigencia,
      });
      setForm(vazio);
      carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao cadastrar');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold">Parâmetros</h1>
        <p className="text-sm text-grafite mt-1">
          Valor do combustível e jornada padrão, usados no cálculo de custo estimado (RN07).
          Cadastre um parâmetro com vigência anterior ou igual à data do roteiro antes de montá-lo.
        </p>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-4">
          {erro && <Alert tipo="erro">{erro}</Alert>}
          <div className="grid grid-cols-3 gap-4">
            <Field label="Valor do combustível (R$/litro)">
              <Input required type="number" step="0.01" min="0.01" value={form.valorCombustivel}
                onChange={(e) => setForm({ ...form, valorCombustivel: e.target.value })} />
            </Field>
            <Field label="Jornada padrão (minutos)">
              <Input required type="number" value={form.jornadaPadraoMinutos}
                onChange={(e) => setForm({ ...form, jornadaPadraoMinutos: e.target.value })} />
            </Field>
            <Field label="Vigente a partir de">
              <Input required type="date" value={form.dataInicioVigencia}
                onChange={(e) => setForm({ ...form, dataInicioVigencia: e.target.value })} />
            </Field>
          </div>
          <Button type="submit" disabled={carregando}>{carregando ? 'Cadastrando...' : 'Cadastrar parâmetro'}</Button>
        </form>
      </Card>

      <Card>
        <h2 className="text-sm font-medium text-grafite mb-3">Parâmetros cadastrados</h2>
        <table className="w-full text-sm">
          <tbody>
            {parametros.map((p) => (
              <tr key={p.id} className="border-t border-neutral-100">
                <td className="py-2 num text-grafite">#{p.id}</td>
                <td className="py-2 font-medium num">R$ {p.valorCombustivel.toFixed(2)}/L</td>
                <td className="py-2 text-grafite num">{p.jornadaPadraoMinutos} min</td>
                <td className="py-2 text-grafite num">desde {p.dataInicioVigencia}</td>
              </tr>
            ))}
            {parametros.length === 0 && <tr><td className="py-3 text-grafite">Nenhum parâmetro cadastrado ainda.</td></tr>}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
