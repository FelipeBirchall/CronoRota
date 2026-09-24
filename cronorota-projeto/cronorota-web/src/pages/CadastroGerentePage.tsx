import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Gerente } from '../types';
import { Alert, Button, Card, Field, Input } from '../components/ui';

export function CadastroGerentePage() {
  const [gerentes, setGerentes] = useState<Gerente[]>([]);
  const [form, setForm] = useState({ nome: '', telefone: '', email: '', login: '', senha: '' });
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  function carregar() {
    api.get<Gerente[]>('/gerentes').then(setGerentes).catch(() => {});
  }

  useEffect(carregar, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      await api.post('/gerentes', form);
      setForm({ nome: '', telefone: '', email: '', login: '', senha: '' });
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
        <h1 className="text-xl font-semibold">Gerentes</h1>
        <p className="text-sm text-grafite mt-1">
          Cadastre um gerente antes de cadastrar motoristas - todo motorista precisa estar
          vinculado a um.
        </p>
      </div>

      <Card>
        <form onSubmit={handleSubmit} className="space-y-4">
          {erro && <Alert tipo="erro">{erro}</Alert>}
          <div className="grid grid-cols-2 gap-4">
            <Field label="Nome">
              <Input required value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} />
            </Field>
            <Field label="Telefone">
              <Input required value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} />
            </Field>
            <Field label="E-mail">
              <Input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </Field>
            <Field label="Login">
              <Input required value={form.login} onChange={(e) => setForm({ ...form, login: e.target.value })} />
            </Field>
            <Field label="Senha">
              <Input required type="password" value={form.senha} onChange={(e) => setForm({ ...form, senha: e.target.value })} />
            </Field>
          </div>
          <Button type="submit" disabled={carregando}>
            {carregando ? 'Cadastrando...' : 'Cadastrar gerente'}
          </Button>
        </form>
      </Card>

      <Card>
        <h2 className="text-sm font-medium text-grafite mb-3">Gerentes cadastrados</h2>
        <table className="w-full text-sm">
          <tbody>
            {gerentes.map((g) => (
              <tr key={g.id} className="border-t border-neutral-100">
                <td className="py-2 num text-grafite">#{g.id}</td>
                <td className="py-2 font-medium">{g.nome}</td>
                <td className="py-2 text-grafite">{g.email}</td>
              </tr>
            ))}
            {gerentes.length === 0 && (
              <tr>
                <td className="py-3 text-grafite">Nenhum gerente cadastrado ainda.</td>
              </tr>
            )}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
