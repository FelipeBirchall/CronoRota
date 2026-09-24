package com.cronorota.repository;

import com.cronorota.model.Parametro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;

public interface ParametroRepository extends JpaRepository<Parametro, Long> {

    /**
     * RN14: pega o parametro vigente numa data especifica - o roteiro usa a
     * vigencia da SUA data, nao a vigencia "de hoje", pra recalculos
     * retroativos ficarem corretos (UC08-A1).
     *
     * Usamos @Query em vez de nome de metodo derivado porque a condicao
     * "dataFimVigencia e nula OU e depois da data" nao tem traducao direta
     * e legivel pelo padrao de nomes do Spring Data.
     */
    @Query("""
            SELECT p FROM Parametro p
            WHERE p.dataInicioVigencia <= :data
            AND (p.dataFimVigencia IS NULL OR p.dataFimVigencia >= :data)
            """)
    Optional<Parametro> buscarVigenteEm(@Param("data") LocalDate data);

    // O parâmetro "em aberto" (sem fim de vigência) - no máximo um, já que o
    // ParametroService encerra o anterior sempre que grava um novo (RN14).
    Optional<Parametro> findByDataFimVigenciaIsNull();

    // RN14 / UC11-E2: já existe parâmetro começando nesta data ou depois?
    // Se sim, a nova vigência se sobreporia a ele.
    boolean existsByDataInicioVigenciaGreaterThanEqual(LocalDate data);
}
