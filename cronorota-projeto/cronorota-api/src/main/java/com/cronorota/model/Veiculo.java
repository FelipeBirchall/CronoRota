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

/**
 * Veículo do motorista. O rendimentoKmLitro é o dado mais sensível daqui -
 * alimenta diretamente o cálculo de custo (RN07), por isso a validação de
 * "maior que zero" (RN10) mora no service, não só numa anotação de bean
 * validation, pra garantir a regra também em updates feitos via service
 * interno (não só via requisição HTTP validada por @Valid).
 */
@Entity
@Audited
@Table(name = "veiculo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String placa;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private String tipo; // ex.: "moto", "carro", "van" - modelo conceitual não tipifica mais que isso

    // BigDecimal, não double - dado financeiro/de cálculo nunca deve usar
    // ponto flutuante binário, por causa de erro de arredondamento acumulado.
    @Column(name = "rendimento_km_litro", nullable = false, precision = 10, scale = 2)
    private BigDecimal rendimentoKmLitro;
}
