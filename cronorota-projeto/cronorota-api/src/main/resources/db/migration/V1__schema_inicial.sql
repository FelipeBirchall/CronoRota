-- Schema inicial do CronoRota. Corresponde diretamente às entidades em
-- com.cronorota.model - o Hibernate está configurado com ddl-auto: validate,
-- então se este arquivo divergir das entidades, a aplicação falha ao subir
-- (de propósito: é melhor falhar cedo aqui do que descobrir a divergência
-- em produção).

-- ========== Usuario (herança JOINED) ==========
CREATE TABLE usuario (
    id            BIGSERIAL PRIMARY KEY,
    nome          VARCHAR(150) NOT NULL,
    telefone      VARCHAR(20)  NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    login         VARCHAR(50)  NOT NULL UNIQUE,
    senha_hash    VARCHAR(255) NOT NULL,
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE gerente (
    id BIGINT PRIMARY KEY REFERENCES usuario(id)
);

CREATE TABLE administrador (
    id BIGINT PRIMARY KEY REFERENCES usuario(id)
);

CREATE TABLE veiculo (
    id                  BIGSERIAL PRIMARY KEY,
    placa               VARCHAR(10)    NOT NULL UNIQUE,
    modelo              VARCHAR(100)   NOT NULL,
    tipo                VARCHAR(30)    NOT NULL,
    rendimento_km_litro NUMERIC(10,2)  NOT NULL CHECK (rendimento_km_litro > 0) -- RN10
);

CREATE TABLE motorista (
    id           BIGINT PRIMARY KEY REFERENCES usuario(id),
    documento    VARCHAR(20)  NOT NULL UNIQUE,
    habilitacao  VARCHAR(20)  NOT NULL,
    veiculo_id   BIGINT       NOT NULL REFERENCES veiculo(id),
    gerente_id   BIGINT       NOT NULL REFERENCES gerente(id)
);

-- ========== Endereco / Pedido / Roteiro / Ponto ==========
CREATE TABLE endereco (
    id          BIGSERIAL PRIMARY KEY,
    logradouro  VARCHAR(200)     NOT NULL,
    bairro      VARCHAR(100)     NOT NULL,
    cidade      VARCHAR(100)     NOT NULL,
    uf          VARCHAR(2)       NOT NULL, -- VARCHAR, não CHAR: o Hibernate mapeia todo campo
                                            -- String da entidade como VARCHAR por padrão, e a
                                            -- validação de schema é estrita sobre isso
    cep         VARCHAR(9)       NOT NULL,
    latitude    NUMERIC(10,7),
    longitude   NUMERIC(10,7)
);

CREATE TABLE roteiro (
    id                        BIGSERIAL PRIMARY KEY,
    data                      DATE           NOT NULL,
    motorista_id              BIGINT         NOT NULL REFERENCES motorista(id),
    gerente_id                BIGINT         NOT NULL REFERENCES gerente(id),
    distancia_total_km        NUMERIC(10,2),
    tempo_total_parado_minutos INTEGER,
    percentual_jornada        NUMERIC(5,2),
    custo_estimado            NUMERIC(10,2),
    ativo                     BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE ponto (
    id                    BIGSERIAL PRIMARY KEY,
    roteiro_id            BIGINT        NOT NULL REFERENCES roteiro(id) ON DELETE CASCADE,
    ordem                 INTEGER       NOT NULL,
    endereco_id           BIGINT        NOT NULL REFERENCES endereco(id),
    data_hora_chegada     TIMESTAMPTZ,  -- timestamptz: guarda o instante com fuso (RN02, seção 25.2 do doc)
    data_hora_saida       TIMESTAMPTZ,
    tempo_parado_minutos  INTEGER,
    UNIQUE (roteiro_id, ordem) -- RN06: ordem sequencial única dentro do roteiro
);

CREATE TABLE pedido (
    id                 BIGSERIAL PRIMARY KEY,
    codigo             VARCHAR(30)   NOT NULL UNIQUE,
    destinatario       VARCHAR(150)  NOT NULL,
    endereco_id        BIGINT        NOT NULL REFERENCES endereco(id),
    data_prevista      DATE          NOT NULL,
    janela_entrega     VARCHAR(50),
    situacao           VARCHAR(20)   NOT NULL,
    ponto_id           BIGINT REFERENCES ponto(id)
);

CREATE TABLE parametro (
    id                        BIGSERIAL PRIMARY KEY,
    valor_combustivel         NUMERIC(10,2) NOT NULL,
    custo_por_km              NUMERIC(10,2),
    jornada_padrao_minutos    INTEGER       NOT NULL DEFAULT 480,
    data_inicio_vigencia      DATE          NOT NULL,
    data_fim_vigencia         DATE -- NULL = ainda vigente (RN14)
);

-- Índices compostos citados na seção 25.2 do documento, pensados pra
-- sustentar as consultas de histórico/dashboard dentro dos 3 segundos (RNF03).
CREATE INDEX idx_roteiro_motorista_data ON roteiro (motorista_id, data);
CREATE INDEX idx_ponto_roteiro_ordem ON ponto (roteiro_id, ordem);
