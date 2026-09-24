// Datas trafegam como texto AAAA-MM-DD (o formato do <input type="date"> e
// do LocalDate do back-end). As contas abaixo usam o meio-dia local para que
// nenhuma mudança de fuso empurre a data para o dia vizinho.

function paraData(iso: string): Date {
  const [ano, mes, dia] = iso.split('-').map(Number);
  return new Date(ano, mes - 1, dia, 12);
}

function paraIso(data: Date): string {
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}

// Data de hoje no fuso do navegador, no formato do <input type="date">
// (AAAA-MM-DD). Não usar new Date().toISOString().slice(0, 10): isso dá a
// data em UTC, que entre 21h e 0h de Brasília já é o dia seguinte.
export function hojeLocal(): string {
  return paraIso(new Date());
}

export function somarDias(iso: string, dias: number): string {
  const data = paraData(iso);
  data.setDate(data.getDate() + dias);
  return paraIso(data);
}

export function primeiroDiaDoMes(iso: string): string {
  return iso.slice(0, 8) + '01';
}

export function somarMeses(iso: string, meses: number): string {
  const data = paraData(iso);
  data.setDate(1);
  data.setMonth(data.getMonth() + meses);
  return paraIso(data);
}

// UC09-E3: períodos acima de 12 meses pedem confirmação (RNF03). De
// 24/09/2025 a 23/09/2026 são 12 meses exatos; a partir de 24/09/2026, passa.
export function passaDe12Meses(inicio: string, fim: string): boolean {
  const limite = paraData(inicio);
  limite.setFullYear(limite.getFullYear() + 1);
  return paraData(fim) >= limite;
}

// "2026-09-24" -> "24/09/2026"; "2026-09" -> "set/2026"
export function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
}

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

export function formatarMes(chave: string): string {
  const [ano, mes] = chave.split('-');
  return `${MESES[Number(mes) - 1]}/${ano}`;
}

// Horários sempre no fuso da operação (seção 24.2 do documento), não no
// fuso do aparelho - é o mesmo que o back-end usa nos relatórios (UC14),
// então tela e arquivo exportado nunca divergem.
export const FUSO_OPERACAO = 'America/Sao_Paulo';

export function formatarHora(instante: string | null): string {
  if (!instante) return '—';
  return new Date(instante).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', timeZone: FUSO_OPERACAO });
}

// Instante -> valor de <input type="datetime-local"> ("2026-09-24T09:15") no
// fuso da operação, para o gerente editar o horário como ele aparece na tela.
export function paraInputDataHora(instante: string | null): string {
  if (!instante) return '';
  const partes = new Intl.DateTimeFormat('en-CA', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
    hourCycle: 'h23', timeZone: FUSO_OPERACAO,
  }).formatToParts(new Date(instante));
  const p = (tipo: string) => partes.find((x) => x.type === tipo)?.value ?? '';
  return `${p('year')}-${p('month')}-${p('day')}T${p('hour')}:${p('minute')}`;
}

export function formatarDataHora(instante: string): string {
  return new Date(instante).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', timeZone: FUSO_OPERACAO,
  });
}

// 161 -> "2h 41min"; 45 -> "45min"
export function formatarMinutos(min: number | null): string {
  if (min === null) return '—';
  const inteiro = Math.round(min);
  const h = Math.floor(inteiro / 60);
  const m = inteiro % 60;
  return h > 0 ? `${h}h ${m}min` : `${m}min`;
}
