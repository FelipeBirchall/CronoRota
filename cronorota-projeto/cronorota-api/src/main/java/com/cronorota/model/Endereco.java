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
import java.math.BigDecimal;

/**
 * Entidade à parte de Ponto (não embutida nele), porque o mesmo endereço pode
 * ser reaproveitado por vários pontos, de roteiros diferentes, sem duplicar
 * linha - é a relação "Ponto localiza-se em Endereco (0..* : 1)" do diagrama
 * conceitual (seção 12.2).
 */
@Entity
@Table(name = "endereco")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String logradouro;

    @Column(nullable = false)
    private String bairro;

    @Column(nullable = false)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(nullable = false)
    private String cep;

    // RN11: todo ponto deve possuir endereço completo e coordenadas válidas
    // antes de entrar em um roteiro - por isso latitude/longitude não são
    // opcionais aqui, mesmo que o cadastro do endereço em si pudesse, a
    // princípio, existir sem coordenada (ex.: endereço digitado manualmente
    // antes da geocodificação responder). Ver observação no ClienteGeocodificacao
    // do service de Ponto sobre esse fluxo.
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
}
