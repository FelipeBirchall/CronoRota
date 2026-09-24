package com.cronorota.auditoria;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Como uma entidade auditada aparece na trilha: nome em português, como
 * obter o id e quais campos mostrar, já formatados para leitura. Os mapas
 * de duas versões da mesma entidade são comparados campo a campo para
 * produzir o "valor anterior / valor novo" do RegistroAuditoria (seção 13).
 *
 * @param chave  identificador usado no filtro da API (ex.: "PONTO")
 * @param nome   nome exibido (ex.: "Ponto")
 * @param campos rótulo do campo -> valor formatado (nulo = vazio); a ordem
 *               do mapa é a ordem de exibição
 */
public record DescritorAuditoria<T>(
        String chave,
        String nome,
        Class<T> classe,
        Function<T, Long> id,
        Function<T, Map<String, String>> campos
) {

    Map<String, String> camposDe(Object entidade) {
        if (entidade == null) {
            return new LinkedHashMap<>();
        }
        return campos.apply(classe.cast(entidade));
    }

    Long idDe(Object entidade) {
        return id.apply(classe.cast(entidade));
    }
}
