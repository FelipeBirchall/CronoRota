package com.cronorota.controller;

import com.cronorota.auditoria.AuditoriaService;
import com.cronorota.auditoria.RegistroExportacaoRepository;
import com.cronorota.dto.response.AlteracaoResponse;
import com.cronorota.dto.response.RegistroExportacaoResponse;
import com.cronorota.relatorio.Formatacao;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

// RNF05 - consulta da trilha de auditoria, só para o administrador (ver
// SecurityConfig). A gravação não tem endpoint: acontece sozinha.
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final RegistroExportacaoRepository registroExportacaoRepository;

    @GetMapping("/alteracoes")
    public List<AlteracaoResponse> alteracoes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) Integer limite) {
        return auditoriaService.listar(inicio, fim, entidade, limite);
    }

    // UC14 passo 5 - quem exportou o quê.
    @GetMapping("/exportacoes")
    public List<RegistroExportacaoResponse> exportacoes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return registroExportacaoRepository.findByInstanteBetweenOrderByInstanteDesc(
                        inicio.atStartOfDay(Formatacao.FUSO).toOffsetDateTime(),
                        fim.plusDays(1).atStartOfDay(Formatacao.FUSO).toOffsetDateTime().minusNanos(1))
                .stream().map(RegistroExportacaoResponse::fromEntity).toList();
    }
}
