package com.cronorota.repository;

import com.cronorota.model.Motorista;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// JpaRepository já entrega save, findById, findAll, delete, etc. prontos.
// Só declaramos aqui os métodos de consulta que o Spring Data gera
// automaticamente a partir do nome do método - sem escrever SQL nenhum.
public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    Optional<Motorista> findByLogin(String login);

    boolean existsByDocumento(String documento);

    boolean existsByLogin(String login);

    // RN13: o gerente enxerga só os motoristas da própria equipe.
    List<Motorista> findByGerente_Id(Long gerenteId);
}
