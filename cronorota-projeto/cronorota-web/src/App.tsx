import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { RequireRole } from './auth/RequireRole';
import { LayoutGerente } from './components/LayoutGerente';
import { LayoutAdmin } from './components/LayoutAdmin';
import { LoginPage } from './pages/LoginPage';
import { BootstrapAdminPage } from './pages/BootstrapAdminPage';
import { CadastroGerentePage } from './pages/CadastroGerentePage';
import { CadastroMotoristaPage } from './pages/CadastroMotoristaPage';
import { CadastroEnderecoPage } from './pages/CadastroEnderecoPage';
import { CadastroPedidoPage } from './pages/CadastroPedidoPage';
import { CadastroParametroPage } from './pages/CadastroParametroPage';
import { MontarRoteiroPage } from './pages/MontarRoteiroPage';
import { RoteiroDetalhePage } from './pages/RoteiroDetalhePage';
import { RoteiroDoDiaPage } from './pages/RoteiroDoDiaPage';
import { MeusRoteirosPage } from './pages/MeusRoteirosPage';
import { DashboardPage } from './pages/DashboardPage';
import { HistoricoMotoristaPage, HistoricoPage } from './pages/HistoricoPage';

const destinoPorPerfil: Record<string, string> = {
  ADMINISTRADOR: '/admin/gerentes',
  GERENTE: '/gerente/dashboard', // UC01 passo 5: o gerente cai no dashboard
  MOTORISTA: '/motorista/meus-roteiros',
};

// A raiz do site não tem conteúdo próprio - ela só decide pra onde mandar
// a pessoa: /login se não houver sessão, ou a home do perfil dela se houver.
function Raiz() {
  const { sessao } = useAuth();
  if (!sessao) return <Navigate to="/login" replace />;
  return <Navigate to={destinoPorPerfil[sessao.perfil]} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Raiz />} />
      <Route path="/login" element={<LoginPage />} />
      {/* Rota de bootstrap: cria o primeiro administrador. Sem proteção de
          perfil porque, num banco vazio, ninguém tem token ainda - o mesmo
          "buraco" já documentado no AdministradorController do back-end. */}
      <Route path="/bootstrap-admin" element={<BootstrapAdminPage />} />

      <Route path="/admin" element={<RequireRole perfis={['ADMINISTRADOR']}><LayoutAdmin /></RequireRole>}>
        <Route path="gerentes" element={<CadastroGerentePage />} />
        <Route path="parametros" element={<CadastroParametroPage />} />
      </Route>

      <Route path="/gerente" element={<RequireRole perfis={['GERENTE']}><LayoutGerente /></RequireRole>}>
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="historico" element={<HistoricoPage />} />
        <Route path="motoristas" element={<CadastroMotoristaPage />} />
        <Route path="enderecos" element={<CadastroEnderecoPage />} />
        <Route path="pedidos" element={<CadastroPedidoPage />} />
        <Route path="roteiros/novo" element={<MontarRoteiroPage />} />
        <Route path="roteiros/:id" element={<RoteiroDetalhePage />} />
      </Route>

      <Route path="/motorista/meus-roteiros" element={<RequireRole perfis={['MOTORISTA']}><MeusRoteirosPage /></RequireRole>} />
      <Route path="/motorista/historico" element={<RequireRole perfis={['MOTORISTA']}><HistoricoMotoristaPage /></RequireRole>} />
      {/* Aberta pra MOTORISTA (uso real) e GERENTE (link "ver como o
          motorista veria" na tela de detalhe do roteiro). */}
      <Route path="/motorista/roteiro/:id" element={<RequireRole perfis={['MOTORISTA', 'GERENTE']}><RoteiroDoDiaPage /></RequireRole>} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
