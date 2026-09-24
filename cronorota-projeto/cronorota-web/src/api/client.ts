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

// Parte comum a JSON e download: cabeçalhos, token e tratamento de erro.
async function enviar(path: string, options?: RequestInit): Promise<Response> {
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

  if (response.status === 401) {
    // Token ausente/expirado/inválido - desloga localmente e manda pro
    // login. Só no 401: o 403 significa "sessão válida, mas este recurso
    // não é seu" (RN13) e cai no tratamento de erro normal abaixo, sem
    // derrubar a sessão.
    localStorage.removeItem(CHAVE_STORAGE);
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
  }

  if (!response.ok) {
    const erro: Partial<ErroApi> = await response.json().catch(() => ({}));
    throw new Error(
      erro.mensagem ?? (response.status === 403 ? 'Você não tem permissão para esta operação' : 'Erro inesperado na API')
    );
  }

  return response;
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await enviar(path, options);
  if (response.status === 204) return undefined as T;
  return response.json();
}

// Download de arquivo (UC14). Não dá para usar um <a href> simples porque
// a API exige o token no cabeçalho; então baixamos o arquivo com fetch e
// entregamos ao navegador por um link temporário. O nome vem do
// Content-Disposition que o back-end manda.
async function baixar(path: string, nomePadrao: string): Promise<void> {
  const response = await enviar(path);
  const disposicao = response.headers.get('Content-Disposition') ?? '';
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(disposicao);
  const simples = /filename="?([^";]+)"?/i.exec(disposicao);
  const nome = utf8 ? decodeURIComponent(utf8[1]) : simples ? simples[1] : nomePadrao;

  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  baixar,
};
