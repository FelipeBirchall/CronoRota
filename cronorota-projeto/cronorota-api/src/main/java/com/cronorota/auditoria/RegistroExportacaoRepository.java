package com.cronorota.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;

public interface RegistroExportacaoRepository extends JpaRepository<RegistroExportacao, Long> {

    List<RegistroExportacao> findByInstanteBetweenOrderByInstanteDesc(OffsetDateTime inicio, OffsetDateTime fim);
}
