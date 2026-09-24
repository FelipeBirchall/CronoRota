-- UC07-A3: o ajuste manual de horário pelo gerente "exige justificativa e é
-- gravado na auditoria". A justificativa fica na própria revisão, junto de
-- quem fez e quando - nula nas revisões que não precisam de uma.
ALTER TABLE revisao ADD COLUMN justificativa VARCHAR(500);
