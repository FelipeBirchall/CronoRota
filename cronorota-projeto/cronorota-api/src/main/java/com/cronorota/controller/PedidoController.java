package com.cronorota.controller;

import com.cronorota.dto.request.CadastrarPedidoRequest;
import com.cronorota.dto.response.PedidoResponse;
import com.cronorota.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponse> cadastrar(@Valid @RequestBody CadastrarPedidoRequest request) {
        var pedido = pedidoService.cadastrar(request.codigo(), request.destinatario(),
                request.enderecoId(), request.dataPrevista(), request.janelaEntrega());
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.fromEntity(pedido));
    }

    @GetMapping("/pendentes")
    public List<PedidoResponse> listarPendentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return pedidoService.listarPendentes(data).stream().map(PedidoResponse::fromEntity).toList();
    }
}
