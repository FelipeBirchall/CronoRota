import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth, type Perfil } from '../auth/AuthContext';
import { Alert, Button, Field, Input } from '../components/ui';

interface LoginResponse {
  token: string;
  perfil: Perfil;
  usuarioId: number;
  nome: string;
}

const destinoPorPerfil: Record<Perfil, string> = {
  ADMINISTRADOR: '/admin/gerentes',
  GERENTE: '/gerente/dashboard', // UC01 passo 5: o gerente cai no dashboard
  MOTORISTA: '/motorista/meus-roteiros',
};

export function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ login: '', senha: '' });
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      const resposta = await api.post<LoginResponse>('/auth/login', form);
      login(resposta);
      navigate(destinoPorPerfil[resposta.perfil], { replace: true });
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao entrar');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="min-h-screen bg-petroleo flex items-center justify-center p-6">
      <div className="w-full max-w-sm bg-white rounded-lg p-8">
        <div className="mb-6">
          <p className="text-xl font-semibold text-petroleo">CronoRota</p>
          <p className="text-sm text-grafite">Cada minuto parado, visível.</p>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          {erro && <Alert tipo="erro">{erro}</Alert>}
          <Field label="Login">
            <Input required autoFocus value={form.login} onChange={(e) => setForm({ ...form, login: e.target.value })} />
          </Field>
          <Field label="Senha">
            <Input required type="password" value={form.senha} onChange={(e) => setForm({ ...form, senha: e.target.value })} />
          </Field>
          <Button type="submit" disabled={carregando} className="w-full justify-center">
            {carregando ? 'Entrando...' : 'Entrar'}
          </Button>
        </form>
      </div>
    </div>
  );
}
