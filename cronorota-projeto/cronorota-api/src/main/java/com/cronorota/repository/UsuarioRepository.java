package com.cronorota.repository;

import com.cronorota.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

// Consulta a tabela "usuario" inteira (os três perfis de uma vez, graças à
// herança JOINED). Login e e-mail são únicos no banco para TODOS os perfis,
// então a checagem de duplicidade precisa olhar aqui, não só na tabela do
// perfil que está sendo cadastrado - senão um gerente com o mesmo login de
// um motorista passaria na validação e estouraria na constraint do banco.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}
