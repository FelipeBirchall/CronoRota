package com.cronorota.controller;

import com.cronorota.dto.request.AjustarHorarioRequest;
import com.cronorota.dto.request.RegistrarPontoRequest;
import com.cronorota.dto.response.PontoResponse;
import com.cronorota.model.Ponto;
import com.cronorota.service.AjusteHorarioService;
import com.cronorota.service.RegistroPontoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.cronorota.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final AjusteHorarioService ajusteHorarioService;

    @PostMapping("/{id}/chegada")
    public ResponseEntity<PontoResponse> registrarChegada(@PathVariable Long id,
                                                            @Valid @RequestBody RegistrarPontoRequest request,
                                                            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Ponto ponto = registroPontoService.registrarChegada(id, request.dataHora(), usuario);
        return ResponseEntity.ok(PontoResponse.fromEntity(ponto));
    }

    @PostMapping("/{id}/saida")
    public ResponseEntity<PontoResponse> registrarSaida(@PathVariable Long id,
                                                          @Valid @RequestBody RegistrarPontoRequest request,
                                                          @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Ponto ponto = registroPontoService.registrarSaida(id, request.dataHora(), usuario);
        return ResponseEntity.ok(PontoResponse.fromEntity(ponto));
    }

    // UC07-A3 - ajuste manual pelo gerente, com justificativa (vai para a auditoria).
    @PutMapping("/{id}/horarios")
    public ResponseEntity<PontoResponse> ajustarHorarios(@PathVariable Long id,
                                                         @Valid @RequestBody AjustarHorarioRequest request,
                                                         @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Ponto ponto = ajusteHorarioService.ajustar(id, request.dataHoraChegada(), request.dataHoraSaida(),
                request.justificativa(), usuario);
        return ResponseEntity.ok(PontoResponse.fromEntity(ponto));
    }
}
