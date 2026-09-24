import { useState } from 'react';
import { api } from '../api/client';
import type { Ponto } from '../types';
import { paraInputDataHora } from '../utils/data';
import { Alert, Button } from './ui';

const JUSTIFICATIVA_MINIMA = 10; // mesmo mínimo do back-end

// UC07-A3 - o gerente corrige um horário registrado indevidamente. A
// justificativa é obrigatória e vai para a trilha de auditoria junto com o
// horário anterior e o novo.
export function AjusteHorarioForm({
  ponto,
  onSalvo,
  onCancelar,
}: {
  ponto: Ponto;
  onSalvo: () => void;
  onCancelar: () => void;
}) {
  const partida = ponto.ordem === 1;
  const [chegada, setChegada] = useState(paraInputDataHora(ponto.dataHoraChegada));
  const [saida, setSaida] = useState(paraInputDataHora(ponto.dataHoraSaida));
  const [justificativa, setJustificativa] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  const justificativaCurta = justificativa.trim().length < JUSTIFICATIVA_MINIMA;

  async function salvar(e: React.FormEvent) {
    e.preventDefault();
    setErro(null);
    setSalvando(true);
    try {
      // Horário local de Brasília, sem fuso: o back-end interpreta no fuso
      // da operação. Campo vazio = manter o horário atual.
      await api.put(`/pontos/${ponto.id}/horarios`, {
        dataHoraChegada: partida || !chegada ? null : chegada,
        dataHoraSaida: saida || null,
        justificativa: justificativa.trim(),
      });
      onSalvo();
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao ajustar o horário');
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form onSubmit={salvar} className="space-y-3 bg-fundo rounded-md border border-neutral-200 p-4">
      <p className="text-sm font-medium">
        Ajustar horários do ponto {ponto.ordem}{partida && ' (partida: só tem saída)'}
      </p>
      {erro && <Alert tipo="erro">{erro}</Alert>}
      <div className="flex flex-wrap gap-3">
        {!partida && (
          <label className="text-xs text-grafite">
            Chegada
            <input type="datetime-local" value={chegada} onChange={(e) => setChegada(e.target.value)}
              className="block mt-1 rounded-md border border-neutral-300 px-2 py-1.5 text-sm bg-white" />
          </label>
        )}
        <label className="text-xs text-grafite">
          Saída
          <input type="datetime-local" value={saida} onChange={(e) => setSaida(e.target.value)}
            className="block mt-1 rounded-md border border-neutral-300 px-2 py-1.5 text-sm bg-white" />
        </label>
      </div>
      <label className="block text-xs text-grafite">
        Justificativa (obrigatória, fica registrada na auditoria)
        <textarea
          value={justificativa}
          onChange={(e) => setJustificativa(e.target.value)}
          maxLength={500}
          rows={2}
          placeholder="Ex.: motorista esqueceu de registrar a saída; conferido com o cliente"
          className="block w-full mt-1 rounded-md border border-neutral-300 px-3 py-2 text-sm bg-white"
        />
      </label>
      <div className="flex items-center gap-3">
        <Button type="submit" disabled={salvando || justificativaCurta}>
          {salvando ? 'Salvando...' : 'Salvar ajuste'}
        </Button>
        <Button type="button" variant="secondary" onClick={onCancelar}>Cancelar</Button>
        {justificativaCurta && (
          <span className="text-xs text-grafite">Mínimo de {JUSTIFICATIVA_MINIMA} caracteres na justificativa</span>
        )}
      </div>
    </form>
  );
}
