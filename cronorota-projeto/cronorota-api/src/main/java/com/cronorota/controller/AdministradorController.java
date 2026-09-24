package com.cronorota.controller;

import com.cronorota.dto.request.CadastrarAdministradorRequest;
import com.cronorota.dto.response.AdministradorResponse;
import com.cronorota.service.AdministradorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ATENÇÃO - BRECHA DE BOOTSTRAP PROPOSITAL: este endpoint fica aberto
 * (sem exigir token) porque, num banco vazio, ninguém tem token de
 * administrador ainda para criar o primeiro. Isso é aceitável SÓ em
 * ambiente de desenvolvimento/teste. Antes de qualquer deploy real, isso
 * precisa virar um script de seed rodado uma vez fora da API, ou exigir uma
 * chave de bootstrap separada - do jeito que está, qualquer pessoa pode
 * criar uma conta de administrador sem autenticação nenhuma.
 */
@RestController
@RequestMapping("/api/administradores")
@RequiredArgsConstructor
public class AdministradorController {

    private final AdministradorService administradorService;

    @PostMapping
    public ResponseEntity<AdministradorResponse> cadastrar(@Valid @RequestBody CadastrarAdministradorRequest request) {
        var administrador = administradorService.cadastrar(request.nome(), request.telefone(),
                request.email(), request.login(), request.senha());
        return ResponseEntity.status(HttpStatus.CREATED).body(AdministradorResponse.fromEntity(administrador));
    }
}
