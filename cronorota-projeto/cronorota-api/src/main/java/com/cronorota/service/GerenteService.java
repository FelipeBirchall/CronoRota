package com.cronorota.service;

import com.cronorota.exception.RecursoNaoEncontradoException;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Gerente;
import com.cronorota.repository.GerenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

// Implementa o UC03 (Manter gerente/coordenador), na versão simplificada
// usada pra popular dados de teste - sem o fluxo de transferência de equipe
// (UC03-A2), que fica pro próximo incremento.
@Service
@RequiredArgsConstructor
public class GerenteService {

    private final GerenteRepository gerenteRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Gerente cadastrar(String nome, String telefone, String email, String login, String senha) {
        if (gerenteRepository.findAll().stream().anyMatch(g -> g.getLogin().equals(login))) {
            throw new RegraDeNegocioException("Login já cadastrado");
        }
        Gerente gerente = Gerente.builder()
                .nome(nome).telefone(telefone).email(email)
                .login(login).senhaHash(passwordEncoder.encode(senha))
                .ativo(true)
                .build();
        return gerenteRepository.save(gerente);
    }

    public List<Gerente> listarTodos() {
        return gerenteRepository.findAll();
    }

    public Gerente buscarPorId(Long id) {
        return gerenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gerente não encontrado: " + id));
    }
}
