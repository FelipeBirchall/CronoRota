package com.cronorota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Conjunto vigente de valores de custo e jornada (RN14). Cada alteração de
 * parâmetro cria uma NOVA linha com dataInicioVigencia própria, em vez de
 * fazer UPDATE na linha existente - é assim que "alterações não retroagem
 * para períodos de vigência já encerrados" (RN14) fica garantido: o histórico
 * de parâmetros passados nunca muda, só o parâmetro "atual" (dataFimVigencia
 * nula) é que pode ser substituído por um novo registro.
 */
@Entity
@Audited
@Table(name = "parametro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parametro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "valor_combustivel", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorCombustivel;

    @Column(name = "custo_por_km", precision = 10, scale = 2)
    private BigDecimal custoPorKm; // pode ser calculado a partir do valorCombustivel do veículo, ou fixado direto

    // Em minutos (480 = 8h), pra evitar conversão repetida entre horas e
    // minutos espalhada pelos services de cálculo.
    @Column(name = "jornada_padrao_minutos", nullable = false)
    @Builder.Default
    private Integer jornadaPadraoMinutos = 480;

    @Column(name = "data_inicio_vigencia", nullable = false)
    private LocalDate dataInicioVigencia;

    @Column(name = "data_fim_vigencia")
    private LocalDate dataFimVigencia; // nulo = ainda vigente
}
