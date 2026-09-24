package com.cronorota.dto.response;

import com.cronorota.model.Parametro;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ParametroResponse(Long id, BigDecimal valorCombustivel, Integer jornadaPadraoMinutos,
                                 LocalDate dataInicioVigencia, LocalDate dataFimVigencia) {
    public static ParametroResponse fromEntity(Parametro p) {
        return new ParametroResponse(p.getId(), p.getValorCombustivel(), p.getJornadaPadraoMinutos(),
                p.getDataInicioVigencia(), p.getDataFimVigencia());
    }
}
