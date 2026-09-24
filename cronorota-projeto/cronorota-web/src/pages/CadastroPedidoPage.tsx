import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Endereco, Pedido } from '../types';
import { Alert, Button, Card, Field, Input, Select } from '../components/ui';

const vazio = { codigo: '', destinatario: '', enderecoId: '', dataPrevista: '', janelaEntrega: '' };

export function CadastroPedidoPage() {
  const [pedidos, setPedidos] = useState<Pedido[]>([]);
  const [enderecos, setEnderecos] = useState<Endereco[]>([]);
  const [form, setForm] = useState(vazio);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  function carregar() {
    api.get<Pedido[]>('/pedidos/pendentes').then(setPedidos).catch(() => {});
    api.get<Endereco[]>('/enderecos').then(setEnderecos).catch(() => {});
  }
  useEffect(carregar, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      await api.post('/pedidos', { ...form, enderecoId: Number(form.enderecoId) });
      setForm(vazio);
      carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao cadastrar');
    } finally {
      setCarregando(false);
    }
  }

  if (enderecos.length === 0) {
    return <Card><p className="text-sm text-grafite">Cadastre um endereço primeiro na aba "Endereços".</p></Card>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold">Pedidos</h1>
        <p className="text-sm text-grafite mt-1">
          Cada pedido pendente vira um ponto candidato na montagem do roteiro (só pedidos
          pendentes aparecem lá - RN12).
        </p>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-4">
          {erro && <Alert tipo="erro">{erro}</Alert>}
          <div className="grid grid-cols-2 gap-4">
            <Field label="Código"><Input required value={form.codigo} onChange={(e) => setForm({ ...form, codigo: e.target.value })} /></Field>
            <Field label="Destinatário"><Input required value={form.destinatario} onChange={(e) => setForm({ ...form, destinatario: e.target.value })} /></Field>
            <Field label="Endereço de entrega">
              <Select required value={form.enderecoId} onChange={(e) => setForm({ ...form, enderecoId: e.target.value })}>
                <option value="">Selecione...</option>
                {enderecos.map((en) => <option key={en.id} value={en.id}>{en.logradouro}, {en.bairro}</option>)}
              </Select>
            </Field>
            <Field label="Data prevista"><Input required type="date" value={form.dataPrevista} onChange={(e) => setForm({ ...form, dataPrevista: e.target.value })} /></Field>
            <Field label="Janela de entrega (opcional)"><Input value={form.janelaEntrega} onChange={(e) => setForm({ ...form, janelaEntrega: e.target.value })} placeholder="ex.: 14h-18h" /></Field>
          </div>
          <Button type="submit" disabled={carregando}>{carregando ? 'Cadastrando...' : 'Cadastrar pedido'}</Button>
        </form>
      </Card>

      <Card>
        <h2 className="text-sm font-medium text-grafite mb-3">Pedidos pendentes (ainda não roteirizados)</h2>
        <table className="w-full text-sm">
          <tbody>
            {pedidos.map((p) => (
              <tr key={p.id} className="border-t border-neutral-100">
                <td className="py-2 num text-grafite">#{p.id}</td>
                <td className="py-2 font-medium">{p.codigo}</td>
                <td className="py-2 text-grafite">{p.destinatario}</td>
                <td className="py-2 text-grafite">{p.endereco.logradouro}</td>
              </tr>
            ))}
            {pedidos.length === 0 && <tr><td className="py-3 text-grafite">Nenhum pedido pendente.</td></tr>}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
