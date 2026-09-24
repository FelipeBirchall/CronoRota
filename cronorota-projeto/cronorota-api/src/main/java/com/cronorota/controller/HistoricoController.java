package com.cronorota.controller;

import com.cronorota.dto.response.HistoricoResponse;
import com.cronorota.security.UsuarioAutenticado;
import com.cronorota.relatorio.FormatoRelatorio;
import com.cronorota.service.ExportacaoService;
import com.cronorota.service.HistoricoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

// UC09 e UC14 - abertos aos três perfis; o que cada um enxerga (RN13) é decidido
// no RoteiroService.buscarNoPeriodo.
@RestController
@RequestMapping("/api/historico")
@RequiredArgsConstructor
public class HistoricoController {

    private final HistoricoService historicoService;
    private final ExportacaoService exportacaoService;

    @GetMapping
    public HistoricoResponse consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long motoristaId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return historicoService.consultar(usuario, inicio, fim, motoristaId);
    }

    // UC14 - o mesmo histórico da tela, como arquivo para download.
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long motoristaId,
            @RequestParam String formato,
            @RequestParam(defaultValue = "false") boolean maiorTempo,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        var arquivo = exportacaoService.exportar(usuario, inicio, fim, motoristaId,
                FormatoRelatorio.de(formato), maiorTempo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(arquivo.nomeArquivo()).build().toString()) // nome só com ASCII
                .contentType(MediaType.parseMediaType(arquivo.contentType()))
                .body(arquivo.conteudo());
    }
}
