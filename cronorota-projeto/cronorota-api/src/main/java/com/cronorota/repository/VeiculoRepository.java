package com.cronorota.repository;

import com.cronorota.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    boolean existsByPlaca(String placa);

    Optional<Veiculo> findByPlaca(String placa);
}
