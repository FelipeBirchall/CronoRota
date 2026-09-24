package com.cronorota.controller;

import com.cronorota.dto.response.DashboardResponse;
import com.cronorota.security.UsuarioAutenticado;
import com.cronorota.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

// UC10 - gerente e administrador (ver SecurityConfig); o gerente vê só a
// própria equipe (RN13), filtro aplicado no RoteiroService.buscarNoPeriodo.
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse montar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long motoristaId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return dashboardService.montar(usuario, inicio, fim, motoristaId);
    }
}
