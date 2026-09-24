package com.cronorota.repository;

import com.cronorota.model.Gerente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GerenteRepository extends JpaRepository<Gerente, Long> {
    Optional<Gerente> findByLogin(String login);
}
