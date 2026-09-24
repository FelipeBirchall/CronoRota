package com.cronorota.controller;

import com.cronorota.dto.request.CadastrarEnderecoRequest;
import com.cronorota.dto.response.EnderecoResponse;
import com.cronorota.service.EnderecoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/enderecos")
@RequiredArgsConstructor
public class EnderecoController {

    private final EnderecoService enderecoService;

    @PostMapping
    public ResponseEntity<EnderecoResponse> cadastrar(@Valid @RequestBody CadastrarEnderecoRequest request) {
        var endereco = enderecoService.cadastrar(request.logradouro(), request.bairro(),
                request.cidade(), request.uf(), request.cep());
        return ResponseEntity.status(HttpStatus.CREATED).body(EnderecoResponse.fromEntity(endereco));
    }

    @GetMapping
    public List<EnderecoResponse> listar() {
        return enderecoService.listarTodos().stream().map(EnderecoResponse::fromEntity).toList();
    }
}
