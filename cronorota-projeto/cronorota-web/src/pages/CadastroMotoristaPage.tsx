import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Gerente, Motorista } from '../types';
import { Alert, Button, Card, Field, Input, Select } from '../components/ui';

const vazio = {
  nome: '', telefone: '', email: '', documento: '', habilitacao: '',
  login: '', senha: '', gerenteId: '', placaVeiculo: '', modeloVeiculo: '',
  tipoVeiculo: 'moto', rendimentoKmLitro: '',
};

export function CadastroMotoristaPage() {
  const [motoristas, setMotoristas] = useState<Motorista[]>([]);
  const [gerentes, setGerentes] = useState<Gerente[]>([]);
  const [form, setForm] = useState(vazio);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  function carregar() {
    api.get<Motorista[]>('/motoristas').then(setMotoristas).catch(() => {});
    api.get<Gerente[]>('/gerentes').then(setGerentes).catch(() => {});
  }

  useEffect(carregar, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      await api.post('/motoristas', {
        ...form,
        gerenteId: Number(form.gerenteId),
        rendimentoKmLitro: Number(form.rendimentoKmLitro),
      });
      setForm(vazio);
      carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao cadastrar');
    } finally {
      setCarregando(false);
    }
  }

  if (gerentes.length === 0) {
    return (
      <Card>
        <p className="text-sm text-grafite">
          Cadastre um gerente primeiro na aba "Gerentes" - todo motorista precisa de um.
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold">Motoristas</h1>
        <p className="text-sm text-grafite mt-1">Cadastro de motorista/motoboy e veículo (RN10: rendimento km/litro maior que zero).</p>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-4">
          {erro && <Alert tipo="erro">{erro}</Alert>}
          <div className="grid grid-cols-2 gap-4">
            <Field label="Nome"><Input required value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} /></Field>
            <Field label="Telefone"><Input required value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} /></Field>
            <Field label="E-mail"><Input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
            <Field label="Documento (CPF)"><Input required value={form.documento} onChange={(e) => setForm({ ...form, documento: e.target.value })} /></Field>
            <Field label="Habilitação"><Input required value={form.habilitacao} onChange={(e) => setForm({ ...form, habilitacao: e.target.value })} /></Field>
            <Field label="Gerente responsável">
              <Select required value={form.gerenteId} onChange={(e) => setForm({ ...form, gerenteId: e.target.value })}>
                <option value="">Selecione...</option>
                {gerentes.map((g) => <option key={g.id} value={g.id}>{g.nome}</option>)}
              </Select>
            </Field>
            <Field label="Login"><Input required value={form.login} onChange={(e) => setForm({ ...form, login: e.target.value })} /></Field>
            <Field label="Senha"><Input required type="password" value={form.senha} onChange={(e) => setForm({ ...form, senha: e.target.value })} /></Field>
          </div>

          <hr className="border-neutral-100" />
          <p className="text-sm font-medium text-grafite">Veículo</p>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Placa"><Input required value={form.placaVeiculo} onChange={(e) => setForm({ ...form, placaVeiculo: e.target.value })} /></Field>
            <Field label="Modelo"><Input required value={form.modeloVeiculo} onChange={(e) => setForm({ ...form, modeloVeiculo: e.target.value })} /></Field>
            <Field label="Tipo">
              <Select value={form.tipoVeiculo} onChange={(e) => setForm({ ...form, tipoVeiculo: e.target.value })}>
                <option value="moto">Moto</option>
                <option value="carro">Carro</option>
                <option value="van">Van</option>
              </Select>
            </Field>
            <Field label="Rendimento (km/litro)">
              <Input required type="number" step="0.1" min="0.1" value={form.rendimentoKmLitro}
                onChange={(e) => setForm({ ...form, rendimentoKmLitro: e.target.value })} />
            </Field>
          </div>

          <Button type="submit" disabled={carregando}>{carregando ? 'Cadastrando...' : 'Cadastrar motorista'}</Button>
        </form>
      </Card>

      <Card>
        <h2 className="text-sm font-medium text-grafite mb-3">Motoristas cadastrados</h2>
        <table className="w-full text-sm">
          <tbody>
            {motoristas.map((m) => (
              <tr key={m.id} className="border-t border-neutral-100">
                <td className="py-2 num text-grafite">#{m.id}</td>
                <td className="py-2 font-medium">{m.nome}</td>
                <td className="py-2 text-grafite">{m.placaVeiculo}</td>
              </tr>
            ))}
            {motoristas.length === 0 && <tr><td className="py-3 text-grafite">Nenhum motorista cadastrado ainda.</td></tr>}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
