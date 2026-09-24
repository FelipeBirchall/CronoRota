package com.cronorota.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Trajeto de um dia, de um único motorista (RN05). Os campos totalizadores
 * (tempoTotalParadoMinutos, percentualJornada, custoEstimado) são gravados
 * aqui de forma desnormalizada - o Serviço de Cálculo (UC08/UC13) os recalcula
 * e grava sempre que um ponto muda, em vez de o front-end/consulta ter que
 * somar tudo toda vez que exibe o histórico ou o dashboard (RNF03: resposta
 * em menos de 3 segundos).
 */
// @Getter/@Setter em vez de @Data: Roteiro -> pontos -> Ponto.roteiro é uma
// referência circular, e o equals/hashCode/toString gerados pelo @Data
// entrariam em recursão infinita (StackOverflowError) ao percorrê-la. O mesmo
// vale para Ponto e Pedido. Entidade JPA fica com a igualdade por identidade.
@Entity
@Audited
@Table(name = "roteiro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Roteiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerente_id", nullable = false)
    private Gerente gerentePlanejador;

    // Composição de verdade: o ponto não existe fora do roteiro (seção 12.2).
    // CascadeType.ALL propaga save/delete do roteiro para os pontos; orphanRemoval
    // remove um Ponto do banco se ele for tirado da lista. Na prática, por causa
    // da RN09, o "delete" físico quase nunca acontece - o normal é inativar.
    @OneToMany(mappedBy = "roteiro", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC")
    @Builder.Default
    private List<Ponto> pontos = new ArrayList<>();

    @Column(name = "distancia_total_km", precision = 10, scale = 2)
    private BigDecimal distanciaTotalKm;

    @Column(name = "tempo_total_parado_minutos")
    private Integer tempoTotalParadoMinutos;

    @Column(name = "percentual_jornada", precision = 5, scale = 2)
    private BigDecimal percentualJornada;

    @Column(name = "custo_estimado", precision = 10, scale = 2)
    private BigDecimal custoEstimado;

    // RN09: registros vinculados a roteiros já executados não podem ser
    // excluídos, apenas inativados.
    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;
}
