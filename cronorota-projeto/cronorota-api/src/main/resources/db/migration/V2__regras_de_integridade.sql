-- RN05: um motorista tem no máximo um roteiro ATIVO por data. O RoteiroService
-- já checa isso antes de gravar, mas o índice garante a regra também contra
-- duas montagens simultâneas (o segundo INSERT falha em vez de duplicar).
-- Índice parcial (WHERE ativo): um roteiro inativado (RN09) libera a data.
CREATE UNIQUE INDEX uq_roteiro_motorista_data_ativo ON roteiro (motorista_id, data) WHERE ativo;

-- RN14: só pode existir um parâmetro "em aberto" (sem fim de vigência).
CREATE UNIQUE INDEX uq_parametro_vigente ON parametro ((data_fim_vigencia IS NULL)) WHERE data_fim_vigencia IS NULL;

-- UC07-E1: saída nunca antes da chegada, também no nível do banco.
ALTER TABLE ponto ADD CONSTRAINT ck_ponto_saida_apos_chegada
    CHECK (data_hora_saida IS NULL OR data_hora_chegada IS NULL OR data_hora_saida >= data_hora_chegada);
