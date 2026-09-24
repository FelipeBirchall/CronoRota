package com.cronorota.dto.response;

import com.cronorota.auditoria.RegistroExportacao;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RegistroExportacaoResponse(
        Long id,
        OffsetDateTime instante,
        String login,
        String perfil,
        String formato,
        LocalDate periodoInicio,
        LocalDate periodoFim,
        Long motoristaId,
        int quantidadeLinhas
) {
    public static RegistroExportacaoResponse fromEntity(RegistroExportacao r) {
        return new RegistroExportacaoResponse(r.getId(), r.getInstante(), r.getLogin(), r.getPerfil(), r.getFormato(),
                r.getPeriodoInicio(), r.getPeriodoFim(), r.getMotoristaId(), r.getQuantidadeLinhas());
    }
}
