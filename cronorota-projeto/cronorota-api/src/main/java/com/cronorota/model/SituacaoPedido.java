package com.cronorota.model;

public enum SituacaoPedido {
    PENDENTE,
    EM_ROTEIRO,
    ENTREGUE,
    CANCELADO // RN12: pedidos cancelados não entram na montagem do roteiro
}
