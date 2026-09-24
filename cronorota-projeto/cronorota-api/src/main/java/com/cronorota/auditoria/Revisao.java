package com.cronorota.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import java.time.Instant;

/**
 * Uma revisão da trilha de auditoria (RNF05): o "quem e quando" de um
 * conjunto de alterações gravadas na mesma transação. O "o quê" (valor
 * anterior e novo de cada campo) fica nas tabelas "<entidade>_aud", uma
 * linha por entidade alterada, todas apontando para esta revisão.
 *
 * Juntas, revisão + tabelas _aud são o RegistroAuditoria do modelo
 * conceitual (seção 13): data/hora, usuário, entidade alterada, campo,
 * valor anterior e valor novo.
 */
@Entity
@Table(name = "revisao")
@RevisionEntity(AutorRevisaoListener.class)
@Getter
@Setter
public class Revisao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    private Long id;

    // Epoch em milissegundos - é o tipo que o Envers aceita como timestamp
    // de revisão. Use getInstante() para ler como data/hora.
    @RevisionTimestamp
    @Column(name = "instante_ms", nullable = false)
    private long instanteMs;

    // Nulo quando a alteração não veio de um usuário logado (ex.: cadastro
    // do primeiro administrador, que é público).
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 50)
    private String login;

    @Column(nullable = false, length = 20)
    private String perfil;

    public Instant getInstante() {
        return Instant.ofEpochMilli(instanteMs);
    }
}
