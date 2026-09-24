package com.cronorota.repository;

import com.cronorota.model.Pedido;
import com.cronorota.model.SituacaoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    boolean existsByCodigo(String codigo);

    // Usado no UC06 (montar roteiro): lista os pedidos ainda não roteirizados
    // dentro de uma data - RN12 (pedidos cancelados não entram na montagem)
    // já é respeitada aqui, filtrando por situação na própria query.
    List<Pedido> findBySituacaoAndPonto_IsNull(SituacaoPedido situacao);

    // Mesma consulta, restrita à data prevista - RN12: pedido com data
    // prevista fora da data do roteiro não entra na montagem.
    List<Pedido> findBySituacaoAndPonto_IsNullAndDataPrevista(SituacaoPedido situacao, LocalDate dataPrevista);
}
