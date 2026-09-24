package com.cronorota.service;

import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Checagens comuns ao cadastro dos três perfis (UC02-E2, UC03). Login e
// e-mail são únicos entre TODOS os usuários, não só dentro de um perfil.
@Service
@RequiredArgsConstructor
public class ValidacaoUsuarioService {

    private final UsuarioRepository usuarioRepository;

    public void validarLoginEEmailDisponiveis(String login, String email) {
        if (usuarioRepository.existsByLogin(login)) {
            throw new RegraDeNegocioException("Login já cadastrado");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("E-mail já cadastrado");
        }
    }
}
