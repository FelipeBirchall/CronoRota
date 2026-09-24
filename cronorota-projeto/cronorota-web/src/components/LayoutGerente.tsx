import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

// Gerentes: Motoristas, Endereços, Pedidos, Montar roteiro. Gerentes e
// Parâmetros (configuração) migraram pra /admin/*, fora do alcance deste
// perfil (RN13) - é a separação que a tabela de perfis já previa.
const links = [
  { to: '/gerente/dashboard', label: 'Dashboard' },
  { to: '/gerente/historico', label: 'Histórico' },
  { to: '/gerente/motoristas', label: 'Motoristas' },
  { to: '/gerente/enderecos', label: 'Endereços' },
  { to: '/gerente/pedidos', label: 'Pedidos' },
  { to: '/gerente/roteiros/novo', label: 'Montar roteiro' },
];

export function LayoutGerente() {
  const { sessao, logout } = useAuth();
  // Dashboard e histórico precisam de largura para gráficos e tabelas; as
  // telas de cadastro continuam estreitas, que é melhor para formulário.
  const { pathname } = useLocation();
  const largo = pathname.startsWith('/gerente/dashboard') || pathname.startsWith('/gerente/historico');

  return (
    <div className="min-h-screen flex">
      <aside className="w-56 shrink-0 bg-petroleo text-white p-5 flex flex-col gap-1">
        <div className="mb-6">
          <p className="text-lg font-semibold leading-tight">CronoRota</p>
          <p className="text-xs text-white/70">Cada minuto parado, visível.</p>
        </div>
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) =>
              `px-3 py-2 rounded-md text-sm transition-colors ${
                isActive ? 'bg-white/15 font-medium' : 'text-white/80 hover:bg-white/10'
              }`
            }
          >
            {link.label}
          </NavLink>
        ))}
        <div className="mt-auto pt-4 border-t border-white/10">
          <p className="text-xs text-white/60 mb-1">{sessao?.nome}</p>
          <button onClick={logout} className="text-xs text-white/80 hover:text-white">Sair</button>
        </div>
      </aside>
      <main className={`flex-1 min-w-0 p-8 ${largo ? 'max-w-6xl' : 'max-w-3xl'}`}>
        <Outlet />
      </main>
    </div>
  );
}
