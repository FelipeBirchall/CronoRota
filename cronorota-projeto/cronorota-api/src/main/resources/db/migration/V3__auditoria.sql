-- Trilha de auditoria (RNF05) com Hibernate Envers (seção 25.1 do documento).
--
-- revisao       = quem e quando (uma linha por transação que alterou algo)
-- <tabela>_aud  = o quê: a versão de cada registro naquela revisão
--                 (revtype: 0 inclusão, 1 alteração, 2 remoção)
--
-- As colunas das tabelas _aud espelham as da tabela original (V1), sem NOT
-- NULL - numa remoção o Envers grava só o id. O hash da senha não é auditado.

CREATE TABLE revisao (
    id          BIGSERIAL PRIMARY KEY,
    instante_ms BIGINT      NOT NULL,
    usuario_id  BIGINT,
    login       VARCHAR(50) NOT NULL,
    perfil      VARCHAR(20) NOT NULL
);

CREATE INDEX idx_revisao_instante ON revisao (instante_ms);

-- ========== Usuario (herança JOINED: revtype só na tabela raiz) ==========
CREATE TABLE usuario_aud (
    id       BIGINT   NOT NULL,
    rev      BIGINT   NOT NULL REFERENCES revisao(id),
    revtype  SMALLINT,
    nome     VARCHAR(150),
    telefone VARCHAR(20),
    email    VARCHAR(150),
    login    VARCHAR(50),
    ativo    BOOLEAN,
    PRIMARY KEY (id, rev)
);

CREATE TABLE gerente_aud (
    id  BIGINT NOT NULL,
    rev BIGINT NOT NULL REFERENCES revisao(id),
    PRIMARY KEY (id, rev)
);

CREATE TABLE administrador_aud (
    id  BIGINT NOT NULL,
    rev BIGINT NOT NULL REFERENCES revisao(id),
    PRIMARY KEY (id, rev)
);

CREATE TABLE motorista_aud (
    id          BIGINT NOT NULL,
    rev         BIGINT NOT NULL REFERENCES revisao(id),
    documento   VARCHAR(20),
    habilitacao VARCHAR(20),
    veiculo_id  BIGINT,
    gerente_id  BIGINT,
    PRIMARY KEY (id, rev)
);

CREATE TABLE veiculo_aud (
    id                  BIGINT NOT NULL,
    rev                 BIGINT NOT NULL REFERENCES revisao(id),
    revtype             SMALLINT,
    placa               VARCHAR(10),
    modelo              VARCHAR(100),
    tipo                VARCHAR(30),
    rendimento_km_litro NUMERIC(10,2),
    PRIMARY KEY (id, rev)
);

-- ========== Endereco / Roteiro / Ponto / Pedido / Parametro ==========
CREATE TABLE endereco_aud (
    id         BIGINT NOT NULL,
    rev        BIGINT NOT NULL REFERENCES revisao(id),
    revtype    SMALLINT,
    logradouro VARCHAR(200),
    bairro     VARCHAR(100),
    cidade     VARCHAR(100),
    uf         VARCHAR(2),
    cep        VARCHAR(9),
    latitude   NUMERIC(10,7),
    longitude  NUMERIC(10,7),
    PRIMARY KEY (id, rev)
);

CREATE TABLE roteiro_aud (
    id                         BIGINT NOT NULL,
    rev                        BIGINT NOT NULL REFERENCES revisao(id),
    revtype                    SMALLINT,
    data                       DATE,
    motorista_id               BIGINT,
    gerente_id                 BIGINT,
    distancia_total_km         NUMERIC(10,2),
    tempo_total_parado_minutos INTEGER,
    percentual_jornada         NUMERIC(5,2),
    custo_estimado             NUMERIC(10,2),
    ativo                      BOOLEAN,
    PRIMARY KEY (id, rev)
);

CREATE TABLE ponto_aud (
    id                   BIGINT NOT NULL,
    rev                  BIGINT NOT NULL REFERENCES revisao(id),
    revtype              SMALLINT,
    roteiro_id           BIGINT,
    ordem                INTEGER,
    endereco_id          BIGINT,
    data_hora_chegada    TIMESTAMPTZ,
    data_hora_saida      TIMESTAMPTZ,
    tempo_parado_minutos INTEGER,
    PRIMARY KEY (id, rev)
);

-- Consulta "alterações deste roteiro" (pontos filtrados pelo roteiro).
CREATE INDEX idx_ponto_aud_roteiro ON ponto_aud (roteiro_id);

CREATE TABLE pedido_aud (
    id             BIGINT NOT NULL,
    rev            BIGINT NOT NULL REFERENCES revisao(id),
    revtype        SMALLINT,
    codigo         VARCHAR(30),
    destinatario   VARCHAR(150),
    endereco_id    BIGINT,
    data_prevista  DATE,
    janela_entrega VARCHAR(50),
    situacao       VARCHAR(20),
    ponto_id       BIGINT,
    PRIMARY KEY (id, rev)
);

CREATE TABLE parametro_aud (
    id                     BIGINT NOT NULL,
    rev                    BIGINT NOT NULL REFERENCES revisao(id),
    revtype                SMALLINT,
    valor_combustivel      NUMERIC(10,2),
    custo_por_km           NUMERIC(10,2),
    jornada_padrao_minutos INTEGER,
    data_inicio_vigencia   DATE,
    data_fim_vigencia      DATE,
    PRIMARY KEY (id, rev)
);

-- ========== Exportações de relatório (UC14 passo 5) ==========
-- Exportar não altera nenhuma entidade, então não passa pelo Envers: é um
-- evento próprio, gravado pelo ExportacaoService.
CREATE TABLE registro_exportacao (
    id                BIGSERIAL PRIMARY KEY,
    instante          TIMESTAMPTZ NOT NULL,
    usuario_id        BIGINT      NOT NULL,
    login             VARCHAR(50) NOT NULL,
    perfil            VARCHAR(20) NOT NULL,
    formato           VARCHAR(10) NOT NULL,
    periodo_inicio    DATE        NOT NULL,
    periodo_fim       DATE        NOT NULL,
    motorista_id      BIGINT,
    quantidade_linhas INTEGER     NOT NULL
);

CREATE INDEX idx_registro_exportacao_instante ON registro_exportacao (instante);

-- ========== Estado inicial ==========
-- Registra o que já existe no banco como uma revisão de inclusão feita pela
-- própria migração. Sem isto, a primeira alteração de um registro antigo
-- apareceria na trilha sem "valor anterior".
INSERT INTO revisao (instante_ms, login, perfil)
VALUES ((EXTRACT(EPOCH FROM now()) * 1000)::BIGINT, 'migracao-v3', 'SISTEMA');

INSERT INTO usuario_aud (id, rev, revtype, nome, telefone, email, login, ativo)
    SELECT id, currval('revisao_id_seq'), 0, nome, telefone, email, login, ativo FROM usuario;
INSERT INTO gerente_aud (id, rev) SELECT id, currval('revisao_id_seq') FROM gerente;
INSERT INTO administrador_aud (id, rev) SELECT id, currval('revisao_id_seq') FROM administrador;
INSERT INTO motorista_aud (id, rev, documento, habilitacao, veiculo_id, gerente_id)
    SELECT id, currval('revisao_id_seq'), documento, habilitacao, veiculo_id, gerente_id FROM motorista;
INSERT INTO veiculo_aud (id, rev, revtype, placa, modelo, tipo, rendimento_km_litro)
    SELECT id, currval('revisao_id_seq'), 0, placa, modelo, tipo, rendimento_km_litro FROM veiculo;
INSERT INTO endereco_aud (id, rev, revtype, logradouro, bairro, cidade, uf, cep, latitude, longitude)
    SELECT id, currval('revisao_id_seq'), 0, logradouro, bairro, cidade, uf, cep, latitude, longitude FROM endereco;
INSERT INTO roteiro_aud (id, rev, revtype, data, motorista_id, gerente_id, distancia_total_km,
                         tempo_total_parado_minutos, percentual_jornada, custo_estimado, ativo)
    SELECT id, currval('revisao_id_seq'), 0, data, motorista_id, gerente_id, distancia_total_km,
           tempo_total_parado_minutos, percentual_jornada, custo_estimado, ativo FROM roteiro;
INSERT INTO ponto_aud (id, rev, revtype, roteiro_id, ordem, endereco_id, data_hora_chegada, data_hora_saida,
                       tempo_parado_minutos)
    SELECT id, currval('revisao_id_seq'), 0, roteiro_id, ordem, endereco_id, data_hora_chegada, data_hora_saida,
           tempo_parado_minutos FROM ponto;
INSERT INTO pedido_aud (id, rev, revtype, codigo, destinatario, endereco_id, data_prevista, janela_entrega,
                        situacao, ponto_id)
    SELECT id, currval('revisao_id_seq'), 0, codigo, destinatario, endereco_id, data_prevista, janela_entrega,
           situacao, ponto_id FROM pedido;
INSERT INTO parametro_aud (id, rev, revtype, valor_combustivel, custo_por_km, jornada_padrao_minutos,
                           data_inicio_vigencia, data_fim_vigencia)
    SELECT id, currval('revisao_id_seq'), 0, valor_combustivel, custo_por_km, jornada_padrao_minutos,
           data_inicio_vigencia, data_fim_vigencia FROM parametro;

-- ========== Somente inclusão (seção 28.2) ==========
-- "Registros de revisão não são alterados nem apagados": o banco recusa
-- UPDATE e DELETE nessas tabelas, inclusive vindos de fora da aplicação.
CREATE FUNCTION impedir_alteracao_auditoria() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'A trilha de auditoria é somente para inclusão (tabela %)', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    tabela TEXT;
BEGIN
    FOREACH tabela IN ARRAY ARRAY['revisao', 'usuario_aud', 'gerente_aud', 'administrador_aud', 'motorista_aud',
                                  'veiculo_aud', 'endereco_aud', 'roteiro_aud', 'ponto_aud', 'pedido_aud',
                                  'parametro_aud', 'registro_exportacao']
    LOOP
        EXECUTE format('CREATE TRIGGER trg_somente_inclusao BEFORE UPDATE OR DELETE ON %I '
                       'FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_auditoria()', tabela);
    END LOOP;
END;
$$;
