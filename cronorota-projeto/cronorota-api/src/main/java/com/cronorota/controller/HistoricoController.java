package com.cronorota.controller;

import com.cronorota.dto.response.HistoricoResponse;
import com.cronorota.security.UsuarioAutenticado;
import com.cronorota.service.HistoricoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

// UC09 - aberto aos três perfis; o que cada um enxerga (RN13) é decidido
// no RoteiroService.buscarNoPeriodo.
@RestController
@RequestMapping("/api/historico")
@RequiredArgsConstructor
public class HistoricoController {

    private final HistoricoService historicoService;

    @GetMapping
    public HistoricoResponse consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long motoristaId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return historicoService.consultar(usuario, inicio, fim, motoristaId);
    }
}
