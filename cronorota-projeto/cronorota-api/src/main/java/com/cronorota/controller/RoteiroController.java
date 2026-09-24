package com.cronorota.controller;

import com.cronorota.auditoria.AuditoriaService;
import com.cronorota.dto.request.MontarRoteiroRequest;
import com.cronorota.dto.response.AlteracaoResponse;
import com.cronorota.dto.response.RoteiroResponse;
import com.cronorota.model.Roteiro;
import com.cronorota.service.RoteiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.cronorota.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/roteiros")
@RequiredArgsConstructor
public class RoteiroController {

    private final RoteiroService roteiroService;
    private final AuditoriaService auditoriaService;

    @PostMapping
    public ResponseEntity<RoteiroResponse> montar(@Valid @RequestBody MontarRoteiroRequest request,
                                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Roteiro roteiro = roteiroService.montar(
                usuario.id(), request.motoristaId(), request.data(),
                request.pedidoIds(), request.distanciaTotalKm()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(RoteiroResponse.fromEntity(roteiro));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoteiroResponse> buscarPorId(@PathVariable Long id,
                                                       @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Roteiro roteiro = roteiroService.buscarPorId(id, usuario);
        return ResponseEntity.ok(RoteiroResponse.fromEntity(roteiro));
    }

    // RNF05 - alterações do roteiro e dos seus pontos, para a tela de detalhe.
    @GetMapping("/{id}/alteracoes")
    public List<AlteracaoResponse> alteracoes(@PathVariable Long id,
                                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return auditoriaService.alteracoesDoRoteiro(id, usuario);
    }

    // Usado pela tela "Meus roteiros" do motorista logado (RN13).
    @GetMapping("/meus")
    public List<RoteiroResponse> meus(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return roteiroService.listarDoMotorista(usuario.id()).stream()
                .map(RoteiroResponse::fromEntity)
                .toList();
    }
}
