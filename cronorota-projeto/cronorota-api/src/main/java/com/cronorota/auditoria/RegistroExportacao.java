package com.cronorota.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * UC14 passo 5: "registra a exportação na auditoria". Exportar não altera
 * nenhuma entidade (não há o que o Envers versionar), então o evento tem a
 * sua própria tabela, também somente para inclusão (migração V3). Sem
 * setters: um registro nasce pronto e não muda.
 */
@Entity
@Table(name = "registro_exportacao")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroExportacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private OffsetDateTime instante;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 50)
    private String login;

    @Column(nullable = false, length = 20)
    private String perfil;

    @Column(nullable = false, length = 10)
    private String formato;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodoFim;

    @Column(name = "motorista_id")
    private Long motoristaId;

    @Column(name = "quantidade_linhas", nullable = false)
    private Integer quantidadeLinhas;
}
