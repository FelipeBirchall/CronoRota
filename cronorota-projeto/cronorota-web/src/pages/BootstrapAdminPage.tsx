import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { Alert, Button, Field, Input } from '../components/ui';

// Tela de uso único: cria a primeira conta de administrador, pra existir
// alguém que consiga então cadastrar gerentes. Sem autenticação nenhuma de
// propósito (ver o aviso no AdministradorController do back-end) - depois
// de criar o primeiro admin, não há motivo pra voltar aqui, mas a rota
// continua funcionando (não há como "desligá-la" sem adicionar auth nela,
// o que é justamente o próximo passo de segurança pendente).
export function BootstrapAdminPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ nome: '', telefone: '', email: '', login: '', senha: '' });
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState(false);
  const [carregando, setCarregando] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      await api.post('/administradores', form);
      setSucesso(true);
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao cadastrar');
    } finally {
      setCarregando(false);
    }
  }

  if (sucesso) {
    return (
      <div className="min-h-screen bg-petroleo flex items-center justify-center p-6">
        <div className="w-full max-w-sm bg-white rounded-lg p-8 text-center space-y-4">
          <p className="text-verdeok font-medium">Administrador criado.</p>
          <Button onClick={() => navigate('/login')} className="w-full justify-center">Ir para o login</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-petroleo flex items-center justify-center p-6">
      <div className="w-full max-w-sm bg-white rounded-lg p-8">
        <div className="mb-6">
          <p className="text-xl font-semibold text-petroleo">Criar administrador</p>
          <p className="text-sm text-grafite">Tela de configuração inicial, uso único.</p>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          {erro && <Alert tipo="erro">{erro}</Alert>}
          <Field label="Nome"><Input required value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} /></Field>
          <Field label="Telefone"><Input required value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} /></Field>
          <Field label="E-mail"><Input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
          <Field label="Login"><Input required value={form.login} onChange={(e) => setForm({ ...form, login: e.target.value })} /></Field>
          <Field label="Senha"><Input required type="password" value={form.senha} onChange={(e) => setForm({ ...form, senha: e.target.value })} /></Field>
          <Button type="submit" disabled={carregando} className="w-full justify-center">
            {carregando ? 'Criando...' : 'Criar administrador'}
          </Button>
        </form>
      </div>
    </div>
  );
}
