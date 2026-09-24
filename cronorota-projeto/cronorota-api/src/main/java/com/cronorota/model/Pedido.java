package com.cronorota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDate;

@Entity
@Table(name = "pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String destinatario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_id", nullable = false)
    private Endereco enderecoEntrega;

    @Column(name = "data_prevista", nullable = false)
    private LocalDate dataPrevista;

    @Column(name = "janela_entrega")
    private String janelaEntrega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoPedido situacao;

    // Preenchido quando o pedido entra na montagem de um roteiro (UC06).
    // Fica nulo até lá - é assim que o service de montagem de roteiro sabe
    // quais pedidos ainda estão "pendentes de roteirizar".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ponto_id")
    private Ponto ponto;
}
