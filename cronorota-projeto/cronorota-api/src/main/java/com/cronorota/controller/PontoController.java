package com.cronorota.controller;

import com.cronorota.dto.request.RegistrarPontoRequest;
import com.cronorota.dto.response.PontoResponse;
import com.cronorota.model.Ponto;
import com.cronorota.service.RegistroPontoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// É esta a tela "Roteiro do dia (móvel)" da seção 16 do documento que fala
// com este controller: os botões de registrar chegada/saída do motorista.
@RestController
@RequestMapping("/api/pontos")
@RequiredArgsConstructor
public class PontoController {

    private final RegistroPontoService registroPontoService;

    @PostMapping("/{id}/chegada")
    public ResponseEntity<PontoResponse> registrarChegada(@PathVariable Long id,
                                                            @Valid @RequestBody RegistrarPontoRequest request) {
        Ponto ponto = registroPontoService.registrarChegada(id, request.dataHora());
        return ResponseEntity.ok(PontoResponse.fromEntity(ponto));
    }

    @PostMapping("/{id}/saida")
    public ResponseEntity<PontoResponse> registrarSaida(@PathVariable Long id,
                                                          @Valid @RequestBody RegistrarPontoRequest request) {
        Ponto ponto = registroPontoService.registrarSaida(id, request.dataHora());
        return ResponseEntity.ok(PontoResponse.fromEntity(ponto));
    }
}
