package com.cronorota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

/**
 * Parada do roteiro. O tipo OffsetDateTime (mapeado para timestamptz no
 * PostgreSQL) é a decisão mais importante desta classe: guarda o instante
 * com fuso horário embutido, o que é o que torna RN02 (diferença entre saída
 * e chegada) correto mesmo em paradas que atravessam a meia-noite ou datas
 * de mudança de horário de verão - exatamente como a seção 25.2 do documento
 * justifica a escolha do PostgreSQL.
 */
@Entity
@Table(name = "ponto")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ponto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roteiro_id", nullable = false)
    private Roteiro roteiro;

    // RN06: ordem sequencial (1, 2, 3, 4...) que define o trajeto do dia.
    // Ordem 1 é sempre o ponto de partida (RN01).
    @Column(nullable = false)
    private Integer ordem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_id", nullable = false)
    private Endereco endereco;

    @Column(name = "data_hora_chegada")
    private OffsetDateTime dataHoraChegada;

    @Column(name = "data_hora_saida")
    private OffsetDateTime dataHoraSaida;

    // Calculado pelo TempoParadoService (UC08), nunca setado diretamente
    // por um controller. Fica nulo enquanto o ponto está "em atendimento"
    // (chegada registrada, saída ainda não) - ver UC08-E2 no documento.
    @Column(name = "tempo_parado_minutos")
    private Integer tempoParadoMinutos;
}
