package com.cronorota.service;

import com.cronorota.exception.RecursoNaoEncontradoException;
import com.cronorota.exception.RegraDeNegocioException;
import com.cronorota.model.Gerente;
import com.cronorota.model.Motorista;
import com.cronorota.model.Veiculo;
import com.cronorota.repository.GerenteRepository;
import com.cronorota.repository.MotoristaRepository;
import com.cronorota.repository.VeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

/**
 * Implementa o UC02 (Manter motorista/motoboy).
 *
 * Nota de arquitetura: o Controller nunca fala direto com o Repository.
 * Ele sempre passa pelo Service, que é onde as regras de negócio (RN07,
 * RN09, RN10) são aplicadas - o Controller só traduz HTTP <-> DTO.
 */
@Service
@RequiredArgsConstructor // Lombok gera o construtor com os campos 'final' abaixo -
                          // é assim que a injeção de dependência do Spring acontece
                          // aqui: sem @Autowired em campo, via construtor.
public class MotoristaService {

    private final MotoristaRepository motoristaRepository;
    private final VeiculoRepository veiculoRepository;
    private final GerenteRepository gerenteRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt, configurado em SecurityConfig

    @Transactional
    public Motorista cadastrar(String nome, String telefone, String email, String documento,
                                String habilitacao, String login, String senha,
                                Long gerenteId, String placaVeiculo, String modeloVeiculo,
                                String tipoVeiculo, BigDecimal rendimentoKmLitro) {

        // RN10: rendimento km/litro estritamente maior que zero.
        if (rendimentoKmLitro == null || rendimentoKmLitro.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Rendimento km/litro deve ser maior que zero");
        }

        // Unicidade de documento e login (fluxo de exceção E2 do UC02).
        if (motoristaRepository.existsByDocumento(documento)) {
            throw new RegraDeNegocioException("Já existe um motorista com este documento");
        }
        if (motoristaRepository.existsByLogin(login)) {
            throw new RegraDeNegocioException("Login já cadastrado");
        }

        Gerente gerente = gerenteRepository.findById(gerenteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gerente não encontrado: " + gerenteId));

        Veiculo veiculo;
        if (veiculoRepository.existsByPlaca(placaVeiculo)) {
            // Fluxo alternativo A3 do UC02: reaproveita veículo já cadastrado
            // em vez de criar duplicado com a mesma placa.
            veiculo = veiculoRepository.findAll().stream()
                    .filter(v -> v.getPlaca().equals(placaVeiculo))
                    .findFirst()
                    .orElseThrow();
        } else {
            veiculo = veiculoRepository.save(Veiculo.builder()
                    .placa(placaVeiculo)
                    .modelo(modeloVeiculo)
                    .tipo(tipoVeiculo)
                    .rendimentoKmLitro(rendimentoKmLitro)
                    .build());
        }

        Motorista motorista = Motorista.builder()
                .nome(nome)
                .telefone(telefone)
                .email(email)
                .login(login)
                .senhaHash(passwordEncoder.encode(senha))
                .documento(documento)
                .habilitacao(habilitacao)
                .veiculo(veiculo)
                .gerente(gerente)
                .ativo(true)
                .build();

        return motoristaRepository.save(motorista);
    }

    /**
     * RN09: registros vinculados a roteiros já executados não podem ser
     * excluídos, apenas inativados. Por isso não existe um método
     * "excluir" nesta classe - só este, que muda o campo ativo.
     */
    @Transactional
    public void inativar(Long motoristaId) {
        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Motorista não encontrado: " + motoristaId));
        motorista.setAtivo(false);
        motoristaRepository.save(motorista);
    }

    public Motorista buscarPorId(Long id) {
        return motoristaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Motorista não encontrado: " + id));
    }

    public java.util.List<Motorista> listarTodos() {
        return motoristaRepository.findAll();
    }
}
