package com.cronorota.relatorio;

import com.cronorota.dto.response.HistoricoResponse;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Tudo o que vai para o arquivo exportado (UC14 passos 3 e 4): o histórico
 * já consultado - o MESMO que a tela mostra -, a descrição do filtro e o
 * instante da geração, para o cabeçalho.
 *
 * @param linhas as paradas já na ordem escolhida na tela
 */
public record DadosRelatorio(
        HistoricoResponse historico,
        List<HistoricoResponse.Linha> linhas,
        String filtro,
        ZonedDateTime geradoEm
) {
}
