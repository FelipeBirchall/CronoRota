// Data de hoje no fuso do navegador, no formato do <input type="date">
// (AAAA-MM-DD). Não usar new Date().toISOString().slice(0, 10): isso dá a
// data em UTC, que entre 21h e 0h de Brasília já é o dia seguinte.
export function hojeLocal(): string {
  const agora = new Date();
  const mes = String(agora.getMonth() + 1).padStart(2, '0');
  const dia = String(agora.getDate()).padStart(2, '0');
  return `${agora.getFullYear()}-${mes}-${dia}`;
}
