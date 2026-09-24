package com.cronorota.service;

import com.cronorota.model.Parametro;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustoServiceTest {

    private final CustoService service = new CustoService();

    // Seção 14.4: R$ 6,00/L ÷ 12 km/L = R$ 0,50/km × 48 km = R$ 24,00.
    @Test
    void exemploDaSecao14_4() {
        Parametro parametro = Parametro.builder().valorCombustivel(new BigDecimal("6.00")).build();
        BigDecimal custo = service.calcularCustoEstimado(new BigDecimal("48"), parametro, new BigDecimal("12"));
        assertThat(custo).isEqualByComparingTo("24.00");
    }

    @Test
    void rendimentoZeroEhRecusado_RN10() {
        Parametro parametro = Parametro.builder().valorCombustivel(new BigDecimal("6.00")).build();
        assertThatThrownBy(() -> service.calcularCustoEstimado(new BigDecimal("48"), parametro, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
