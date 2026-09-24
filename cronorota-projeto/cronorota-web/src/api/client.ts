import type { ErroApi } from '../types';

const BASE_URL = 'http://localhost:8080/api';
const CHAVE_STORAGE = 'cronorota_sessao';

function tokenAtual(): string | null {
  const salvo = localStorage.getItem(CHAVE_STORAGE);
  if (!salvo) return null;
  try {
    return JSON.parse(salvo).token ?? null;
  } catch {
    return null;
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const token = tokenAtual();

  const response = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      // Anexa o token em toda chamada, se existir - as rotas de login
      // (/auth/login) e cadastro de administrador ignoram esse cabeçalho
      // por serem públicas no back-end, então não tem problema mandar
      // mesmo quando ainda não há sessão.
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...options,
  });

  if (response.status === 401 || response.status === 403) {
    // Token ausente/expirado/inválido - desloga localmente e manda pro
    // login. Não usamos o AuthContext aqui (este arquivo não é um
    // componente React), então a forma simples é limpar o storage direto
    // e redirecionar via location.
    localStorage.removeItem(CHAVE_STORAGE);
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
  }

  if (!response.ok) {
    const erro: ErroApi = await response.json().catch(() => ({
      timestamp: new Date().toISOString(),
      status: response.status,
      mensagem: 'Erro inesperado na API',
    }));
    throw new Error(erro.mensagem);
  }

  if (response.status === 204) return undefined as T;

  return response.json();
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
};
