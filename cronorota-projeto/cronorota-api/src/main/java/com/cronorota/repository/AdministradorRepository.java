package com.cronorota.repository;

import com.cronorota.model.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
    Optional<Administrador> findByLogin(String login);
    boolean existsByLogin(String login);
}
