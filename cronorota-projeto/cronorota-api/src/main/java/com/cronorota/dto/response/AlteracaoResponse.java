package com.cronorota.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Uma entrada da trilha de auditoria (RNF05): quem alterou, quando, qual
 * registro e, campo a campo, o valor anterior e o novo - o RegistroAuditoria
 * do modelo conceitual (seção 13).
 *
 * @param operacao      INCLUSAO, ALTERACAO ou REMOCAO
 * @param justificativa motivo informado, quando a operação exige um (UC07-A3)
 */
public record AlteracaoResponse(
        Long revisao,
        Instant instante,
        String login,
        String perfil,
        String entidade,
        String entidadeNome,
        Long registroId,
        String operacao,
        String justificativa,
        List<Campo> campos
) {
    public record Campo(String campo, String anterior, String novo) {
    }
}
