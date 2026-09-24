package com.cronorota.auditoria;

import com.cronorota.security.UsuarioAutenticado;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Chamado pelo Envers ao abrir cada revisão: carimba nela o usuário da
 * requisição atual, lido do contexto de segurança (o mesmo que o
 * JwtAuthFilter preencheu a partir do token). É por isso que nenhum service
 * precisa lembrar de "registrar na auditoria" - toda gravação de entidade
 * auditada já sai com autor.
 */
public class AutorRevisaoListener implements RevisionListener {

    static final String SEM_LOGIN = "anonimo";
    static final String PERFIL_SISTEMA = "SISTEMA";

    @Override
    public void newRevision(Object entidadeRevisao) {
        Revisao revisao = (Revisao) entidadeRevisao;
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario) {
            revisao.setUsuarioId(usuario.id());
            revisao.setLogin(usuario.login());
            revisao.setPerfil(usuario.perfil());
        } else {
            revisao.setLogin(SEM_LOGIN);
            revisao.setPerfil(PERFIL_SISTEMA);
        }
    }
}
