import { createContext, useContext, useState, type ReactNode } from 'react';

export type Perfil = 'MOTORISTA' | 'GERENTE' | 'ADMINISTRADOR';

interface Sessao {
  token: string;
  perfil: Perfil;
  usuarioId: number;
  nome: string;
}

interface AuthContextValue {
  sessao: Sessao | null;
  login: (sessao: Sessao) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);
const CHAVE_STORAGE = 'cronorota_sessao';

// A sessão vive em localStorage pra sobreviver a um F5 - sem isso, recarregar
// a página derrubaria o login toda vez. Guardamos o token e os dados que a
// navegação (menu, redirecionamentos) precisa sem chamar a API de novo.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Sessao | null>(() => {
    const salvo = localStorage.getItem(CHAVE_STORAGE);
    return salvo ? JSON.parse(salvo) : null;
  });

  function login(nova: Sessao) {
    localStorage.setItem(CHAVE_STORAGE, JSON.stringify(nova));
    setSessao(nova);
  }

  function logout() {
    localStorage.removeItem(CHAVE_STORAGE);
    setSessao(null);
  }

  return <AuthContext.Provider value={{ sessao, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth precisa estar dentro de um AuthProvider');
  return ctx;
}
