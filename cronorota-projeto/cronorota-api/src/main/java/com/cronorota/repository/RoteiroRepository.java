package com.cronorota.repository;

import com.cronorota.model.Roteiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface RoteiroRepository extends JpaRepository<Roteiro, Long> {

    // RN13: motorista consulta só os seus próprios roteiros; gerente, os da
    // sua equipe. Este método atende o primeiro caso.
    List<Roteiro> findByMotorista_IdAndDataBetween(Long motoristaId, LocalDate inicio, LocalDate fim);

    // Atende o caso do gerente (RN13): roteiros de todos os motoristas da
    // equipe dele, filtrando pelo gerente responsável pelo motorista.
    List<Roteiro> findByMotorista_Gerente_IdAndDataBetween(Long gerenteId, LocalDate inicio, LocalDate fim);

    // Usado pela tela "Meus roteiros" do motorista logado - sem filtro de
    // período, ordenado do mais recente pro mais antigo.
    List<Roteiro> findByMotorista_IdOrderByDataDesc(Long motoristaId);

    // RN05: um motorista tem no máximo um roteiro (ativo) por data.
    boolean existsByMotorista_IdAndDataAndAtivoTrue(Long motoristaId, LocalDate data);

    // RN14 / UC08-A1: roteiros alcançados por um parâmetro que entra em
    // vigência nesta data, e que por isso precisam ter os indicadores refeitos.
    List<Roteiro> findByDataGreaterThanEqualAndAtivoTrue(LocalDate data);

    /**
     * Base do histórico (UC09) e do dashboard (UC10): roteiros ativos do
     * período, já com motorista, pontos e endereços carregados numa consulta
     * só (JOIN FETCH) - sem isso, montar a resposta dispararia uma consulta
     * por roteiro e outra por ponto (o "N+1"), o que derruba o RNF03
     * (dashboard em menos de 3 segundos para 12 meses).
     *
     * gerenteId e motoristaId são filtros opcionais (nulo = sem filtro): é
     * por eles que o RoteiroService aplica a RN13 no período consultado.
     */
    @Query("""
            SELECT DISTINCT r FROM Roteiro r
            JOIN FETCH r.motorista m
            LEFT JOIN FETCH r.pontos p
            LEFT JOIN FETCH p.endereco
            WHERE r.ativo = true
            AND r.data BETWEEN :inicio AND :fim
            AND (:gerenteId IS NULL OR m.gerente.id = :gerenteId)
            AND (:motoristaId IS NULL OR m.id = :motoristaId)
            ORDER BY r.data, r.id
            """)
    List<Roteiro> buscarNoPeriodo(@Param("inicio") LocalDate inicio,
                                  @Param("fim") LocalDate fim,
                                  @Param("gerenteId") Long gerenteId,
                                  @Param("motoristaId") Long motoristaId);
}
