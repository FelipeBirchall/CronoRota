package com.cronorota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "motorista")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Motorista extends Usuario {

    @Column(nullable = false, unique = true)
    private String documento;

    @Column(nullable = false)
    private String habilitacao;

    // FetchType.LAZY: só carrega o veículo do banco quando alguém de fato
    // acessar motorista.getVeiculo() - evita trazer dado que não vai ser usado
    // em toda consulta de motorista (ex.: numa listagem simples de nomes).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerente_id", nullable = false)
    private Gerente gerente;
}
