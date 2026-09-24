package com.cronorota.controller;

import com.cronorota.dto.request.CadastrarMotoristaRequest;
import com.cronorota.dto.response.MotoristaResponse;
import com.cronorota.model.Motorista;
import com.cronorota.service.MotoristaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Repare no padrão que se repete em todo controller deste projeto:
 * 1. Recebe o DTO de request (já validado pelo @Valid antes mesmo do método rodar)
 * 2. Chama o Service, passando dados simples (não o request inteiro) - o
 *    Service não deveria conhecer a existência de um "DTO HTTP"
 * 3. Converte a entidade retornada num DTO de response
 * 4. Devolve com o HttpStatus correto
 *
 * Nenhuma regra de negócio aparece aqui - se você sentir vontade de
 * escrever um "if" de regra dentro de um controller, é sinal de que ele
 * deveria estar no Service.
 */
@RestController
@RequestMapping("/api/motoristas")
@RequiredArgsConstructor
public class MotoristaController {

    private final MotoristaService motoristaService;

    @PostMapping
    public ResponseEntity<MotoristaResponse> cadastrar(@Valid @RequestBody CadastrarMotoristaRequest request) {
        Motorista motorista = motoristaService.cadastrar(
                request.nome(), request.telefone(), request.email(),
                request.documento(), request.habilitacao(),
                request.login(), request.senha(), request.gerenteId(),
                request.placaVeiculo(), request.modeloVeiculo(), request.tipoVeiculo(),
                request.rendimentoKmLitro()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(MotoristaResponse.fromEntity(motorista));
    }

    @GetMapping
    public List<MotoristaResponse> listar() {
        return motoristaService.listarTodos().stream().map(MotoristaResponse::fromEntity).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MotoristaResponse> buscarPorId(@PathVariable Long id) {
        Motorista motorista = motoristaService.buscarPorId(id);
        return ResponseEntity.ok(MotoristaResponse.fromEntity(motorista));
    }

    // PUT, não DELETE: RN09 - inativação, não exclusão.
    @PutMapping("/{id}/inativar")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        motoristaService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
