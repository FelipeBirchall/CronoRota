package com.cronorota.repository;

import com.cronorota.model.Roteiro;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
