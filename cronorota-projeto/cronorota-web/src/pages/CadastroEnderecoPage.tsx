import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Endereco } from '../types';
import { Alert, Button, Card, Field, Input } from '../components/ui';

const vazio = { logradouro: '', bairro: '', cidade: 'Belo Horizonte', uf: 'MG', cep: '' };

export function CadastroEnderecoPage() {
  const [enderecos, setEnderecos] = useState<Endereco[]>([]);
  const [form, setForm] = useState(vazio);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  function carregar() {
    api.get<Endereco[]>('/enderecos').then(setEnderecos).catch(() => {});
  }
  useEffect(carregar, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      await api.post('/enderecos', form);
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
        <h1 className="text-xl font-semibold">Endereços</h1>
        <p className="text-sm text-grafite mt-1">
          Endereços reutilizáveis entre pedidos - o mesmo endereço pode aparecer em vários pontos
          de roteiros diferentes, sem duplicar cadastro.
        </p>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-4">
          {erro && <Alert tipo="erro">{erro}</Alert>}
          <div className="grid grid-cols-2 gap-4">
            <Field label="Logradouro"><Input required value={form.logradouro} onChange={(e) => setForm({ ...form, logradouro: e.target.value })} /></Field>
            <Field label="Bairro"><Input required value={form.bairro} onChange={(e) => setForm({ ...form, bairro: e.target.value })} /></Field>
            <Field label="Cidade"><Input required value={form.cidade} onChange={(e) => setForm({ ...form, cidade: e.target.value })} /></Field>
            <Field label="UF"><Input required maxLength={2} value={form.uf} onChange={(e) => setForm({ ...form, uf: e.target.value.toUpperCase() })} /></Field>
            <Field label="CEP"><Input required value={form.cep} onChange={(e) => setForm({ ...form, cep: e.target.value })} /></Field>
          </div>
          <Button type="submit" disabled={carregando}>{carregando ? 'Cadastrando...' : 'Cadastrar endereço'}</Button>
        </form>
      </Card>

      <Card>
        <h2 className="text-sm font-medium text-grafite mb-3">Endereços cadastrados</h2>
        <table className="w-full text-sm">
          <tbody>
            {enderecos.map((e) => (
              <tr key={e.id} className="border-t border-neutral-100">
                <td className="py-2 num text-grafite">#{e.id}</td>
                <td className="py-2 font-medium">{e.logradouro}, {e.bairro}</td>
                <td className="py-2 text-grafite">{e.cidade}/{e.uf}</td>
              </tr>
            ))}
            {enderecos.length === 0 && <tr><td className="py-3 text-grafite">Nenhum endereço cadastrado ainda.</td></tr>}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
