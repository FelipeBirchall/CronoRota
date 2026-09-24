package com.cronorota.service;

import com.cronorota.model.Parametro;
import com.cronorota.repository.ParametroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Implementa o UC11/UC12 (Parametrizar custos e regras de cálculo), na
// versão mínima necessária pra desbloquear o cálculo de custo (UC13) na
// ponta a ponta. RN14 completa (encerrar a vigência anterior automaticamente
// ao cadastrar uma nova) fica pro próximo incremento - por ora, é
// responsabilidade de quem cadastra não sobrepor vigências.
@Service
@RequiredArgsConstructor
public class ParametroService {

    private final ParametroRepository parametroRepository;

    @Transactional
    public Parametro cadastrar(BigDecimal valorCombustivel, Integer jornadaPadraoMinutos, LocalDate dataInicioVigencia) {
        Parametro parametro = Parametro.builder()
                .valorCombustivel(valorCombustivel)
                .jornadaPadraoMinutos(jornadaPadraoMinutos)
                .dataInicioVigencia(dataInicioVigencia)
                .build();
        return parametroRepository.save(parametro);
    }

    public List<Parametro> listarTodos() {
        return parametroRepository.findAll();
    }
}
