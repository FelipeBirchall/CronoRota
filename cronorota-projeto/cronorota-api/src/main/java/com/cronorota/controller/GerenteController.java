package com.cronorota.controller;

import com.cronorota.dto.request.CadastrarGerenteRequest;
import com.cronorota.dto.response.GerenteResponse;
import com.cronorota.service.GerenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/gerentes")
@RequiredArgsConstructor
public class GerenteController {

    private final GerenteService gerenteService;

    @PostMapping
    public ResponseEntity<GerenteResponse> cadastrar(@Valid @RequestBody CadastrarGerenteRequest request) {
        var gerente = gerenteService.cadastrar(request.nome(), request.telefone(), request.email(),
                request.login(), request.senha());
        return ResponseEntity.status(HttpStatus.CREATED).body(GerenteResponse.fromEntity(gerente));
    }

    @GetMapping
    public List<GerenteResponse> listar() {
        return gerenteService.listarTodos().stream().map(GerenteResponse::fromEntity).toList();
    }
}
