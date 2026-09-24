package com.cronorota.service;

import com.cronorota.model.Administrador;
import com.cronorota.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Implementa o UC03 (Manter gerente/coordenador) na parte de administrador -
// o documento não detalha um caso de uso próprio pra isso, mas alguém
// precisa poder criar o primeiro administrador do sistema.
@Service
@RequiredArgsConstructor
public class AdministradorService {

    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;
    private final ValidacaoUsuarioService validacaoUsuarioService;

    @Transactional
    public Administrador cadastrar(String nome, String telefone, String email, String login, String senha) {
        validacaoUsuarioService.validarLoginEEmailDisponiveis(login, email);
        Administrador administrador = Administrador.builder()
                .nome(nome).telefone(telefone).email(email)
                .login(login).senhaHash(passwordEncoder.encode(senha))
                .ativo(true)
                .build();
        return administradorRepository.save(administrador);
    }
}
