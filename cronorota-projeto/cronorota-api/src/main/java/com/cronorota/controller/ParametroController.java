package com.cronorota.controller;

import com.cronorota.dto.request.CadastrarParametroRequest;
import com.cronorota.dto.response.ParametroResponse;
import com.cronorota.service.ParametroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/parametros")
@RequiredArgsConstructor
public class ParametroController {

    private final ParametroService parametroService;

    @PostMapping
    public ResponseEntity<ParametroResponse> cadastrar(@Valid @RequestBody CadastrarParametroRequest request) {
        var parametro = parametroService.cadastrar(request.valorCombustivel(), request.jornadaPadraoMinutos(), request.dataInicioVigencia());
        return ResponseEntity.status(HttpStatus.CREATED).body(ParametroResponse.fromEntity(parametro));
    }

    @GetMapping
    public List<ParametroResponse> listar() {
        return parametroService.listarTodos().stream().map(ParametroResponse::fromEntity).toList();
    }
}
