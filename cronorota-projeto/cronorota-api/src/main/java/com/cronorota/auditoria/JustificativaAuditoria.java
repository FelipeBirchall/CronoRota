package com.cronorota.auditoria;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anexa uma justificativa à revisão de auditoria da transação em curso.
 *
 * O Envers cria uma revisão por transação e só a grava no commit - depois
 * que o service já retornou. Por isso a justificativa não pode ir por uma
 * variável de thread limpa no fim do método: getCurrentRevision devolve a
 * própria revisão desta transação, e o que for escrito nela é gravado junto.
 */
@Component
@RequiredArgsConstructor
public class JustificativaAuditoria {

    private final EntityManager entityManager;

    // MANDATORY: sem transação não há revisão a que anexar - chamar fora de
    // uma é erro de programação, e o Spring acusa na hora.
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String justificativa) {
        Revisao revisao = AuditReaderFactory.get(entityManager).getCurrentRevision(Revisao.class, false);
        revisao.setJustificativa(justificativa);
    }
}
