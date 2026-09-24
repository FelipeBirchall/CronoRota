package com.cronorota.service;

import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.dto.response.LoginResponse;
import com.cronorota.model.Administrador;
import com.cronorota.model.Gerente;
import com.cronorota.model.Motorista;
import com.cronorota.repository.AdministradorRepository;
import com.cronorota.repository.GerenteRepository;
import com.cronorota.repository.MotoristaRepository;
import com.cronorota.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementa o UC01 (Autenticar no sistema). Como Motorista, Gerente e
 * Administrador são tabelas separadas (herança JOINED - seção model/Usuario.java),
 * não existe uma única consulta "SELECT * FROM usuario WHERE login = ?" que
 * já traga o perfil - por isso a busca tenta as três tabelas em sequência.
 * Login é único globalmente (cada tabela tem a constraint UNIQUE em login),
 * então no máximo uma delas encontra o usuário.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MotoristaRepository motoristaRepository;
    private final GerenteRepository gerenteRepository;
    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse autenticar(String login, String senha) {
        var motorista = motoristaRepository.findByLogin(login);
        if (motorista.isPresent()) {
            return autenticarComo(motorista.get().getId(), motorista.get().getNome(),
                    motorista.get().getSenhaHash(), motorista.get().isAtivo(), senha, "MOTORISTA", login);
        }

        var gerente = gerenteRepository.findByLogin(login);
        if (gerente.isPresent()) {
            return autenticarComo(gerente.get().getId(), gerente.get().getNome(),
                    gerente.get().getSenhaHash(), gerente.get().isAtivo(), senha, "GERENTE", login);
        }

        var administrador = administradorRepository.findByLogin(login);
        if (administrador.isPresent()) {
            return autenticarComo(administrador.get().getId(), administrador.get().getNome(),
                    administrador.get().getSenhaHash(), administrador.get().isAtivo(), senha, "ADMINISTRADOR", login);
        }

        // UC01-E1: mesma mensagem genérica tanto pra login inexistente quanto
        // senha errada - não informar qual dos dois está incorreto (evita dar
        // pista pra quem está tentando adivinhar credenciais).
        throw new RegraDeNegocioException("Usuário ou senha inválidos");
    }

    private LoginResponse autenticarComo(Long id, String nome, String senhaHash, boolean ativo,
                                          String senhaInformada, String perfil, String login) {
        if (!ativo) {
            // UC01-E2: usuário inativo.
            throw new RegraDeNegocioException("Usuário inativo - procure o gerente responsável");
        }
        if (!passwordEncoder.matches(senhaInformada, senhaHash)) {
            throw new RegraDeNegocioException("Usuário ou senha inválidos");
        }
        String token = jwtService.gerarToken(login, id, perfil, nome);
        return new LoginResponse(token, perfil, id, nome);
    }
}
