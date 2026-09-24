package com.cronorota.repository;

import com.cronorota.model.Pedido;
import com.cronorota.model.SituacaoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Usado no UC06 (montar roteiro): lista os pedidos ainda não roteirizados
    // dentro de uma data - RN12 (pedidos cancelados não entram na montagem)
    // já é respeitada aqui, filtrando por situação na própria query.
    List<Pedido> findBySituacaoAndPonto_IsNull(SituacaoPedido situacao);
}
