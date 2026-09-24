import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

// Administrador: Gerentes e Parâmetros - configuração, não operação do dia
// a dia. Não vê Motoristas/Endereços/Pedidos/Montar roteiro, que são do
// Gerente (RN13).
const links = [
  { to: '/admin/gerentes', label: 'Gerentes' },
  { to: '/admin/parametros', label: 'Parâmetros' },
];

export function LayoutAdmin() {
  const { sessao, logout } = useAuth();

  return (
    <div className="min-h-screen flex">
      <aside className="w-56 shrink-0 bg-[#1a1a1a] text-white p-5 flex flex-col gap-1">
        <div className="mb-6">
          <p className="text-lg font-semibold leading-tight">CronoRota</p>
          <p className="text-xs text-white/60">Painel do administrador</p>
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
      <main className="flex-1 p-8 max-w-3xl">
        <Outlet />
      </main>
    </div>
  );
}
