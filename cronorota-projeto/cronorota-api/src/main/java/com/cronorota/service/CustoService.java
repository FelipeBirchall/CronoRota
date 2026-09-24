package com.cronorota.service;

import com.cronorota.model.Parametro;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implementa o UC13 (Calcular custo estimado do roteiro) e a RN07.
 *
 * Fórmula (seção 14.1 e 14.4 do documento):
 *   custo por km = valor do combustível ÷ rendimento km/litro
 *   custo estimado do roteiro = custo por km × distância total
 */
@Service
public class CustoService {

    public BigDecimal calcularCustoEstimado(BigDecimal distanciaTotalKm, Parametro parametro, BigDecimal rendimentoKmLitro) {
        if (rendimentoKmLitro == null || rendimentoKmLitro.compareTo(BigDecimal.ZERO) <= 0) {
            // RN10: rendimento km/litro deve ser estritamente maior que zero.
            // Chegar aqui com valor inválido é um bug em outra camada (a
            // validação de entrada deveria ter barrado antes) - por isso é
            // exceção, não um retorno "silencioso" tipo zero ou null.
            throw new IllegalArgumentException("Rendimento km/litro deve ser maior que zero");
        }

        BigDecimal custoPorKm = parametro.getValorCombustivel()
                .divide(rendimentoKmLitro, 4, RoundingMode.HALF_UP);

        return custoPorKm
                .multiply(distanciaTotalKm)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
