import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth, type Perfil } from './AuthContext';

// Envolve uma árvore de rotas e só deixa passar se a sessão existir E o
// perfil estiver na lista permitida - é isto que impede, por exemplo, um
// motorista de digitar /gerente/motoristas na barra de endereço e ver a
// tela de outro perfil (RN13), mesmo sem essa rota aparecer no menu dele.
export function RequireRole({ perfis, children }: { perfis: Perfil[]; children: ReactNode }) {
  const { sessao } = useAuth();

  if (!sessao) return <Navigate to="/login" replace />;
  if (!perfis.includes(sessao.perfil)) return <Navigate to="/" replace />;

  return <>{children}</>;
}
