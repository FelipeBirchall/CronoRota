package com.cronorota.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Classe abstrata com os dados comuns aos três perfis (seção 12.1 do documento).
 *
 * Estratégia de herança: JOINED. Isso cria uma tabela "usuario" com as colunas
 * comuns, e uma tabela por subtipo (motorista, gerente, administrador) só com
 * as colunas específicas, ligadas por chave estrangeira no id. É a estratégia
 * mais "correta" relacionalmente (sem colunas nulas de outros perfis numa
 * tabela só), ao custo de um JOIN a mais nas consultas - aceitável aqui,
 * já que o volume de usuários é baixo (30 motoristas + poucos gerentes/admins).
 *
 * IMPORTANTE: aqui usamos @Getter/@Setter/@EqualsAndHashCode/@ToString em vez
 * de @Data. @Data e @SuperBuilder NÃO são compatíveis entre si - as duas
 * geram construtores concorrentes, e isso quebra a geração do builder em
 * cascata em Motorista/Gerente/Administrador. Essa combinação aqui é a
 * recomendada pela própria documentação do Lombok para classes-base de uma
 * hierarquia com @SuperBuilder.
 */
@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String login;

    // Nunca armazenar senha em texto claro (RNF08). O hash é gerado no service,
    // nunca aqui na entidade - a entidade só guarda o resultado já hasheado.
    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    // RN09: registros vinculados a roteiros já executados não podem ser excluídos,
    // apenas inativados. Por isso não existe DELETE de usuário em nenhum service -
    // só update deste campo.
    // @Builder.Default: sem isso o @SuperBuilder ignora o "= true" e todo
    // usuário criado pelo builder nasceria inativo se o service esquecesse
    // de chamar .ativo(true).
    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;
}
