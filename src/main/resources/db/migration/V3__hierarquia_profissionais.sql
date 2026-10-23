-- Hierarquia de profissionais em tabela única (SINGLE_TABLE): a coluna tipo é o discriminador
-- e cada categoria com área tem a sua coluna. A especialidade em texto livre deixa de existir.

ALTER TABLE profissional_saude ADD COLUMN tipo VARCHAR(30);
ALTER TABLE profissional_saude ADD COLUMN area_atuacao VARCHAR(30);
ALTER TABLE profissional_saude ADD COLUMN area_cirurgica VARCHAR(30);

-- Não há como inferir a categoria a partir da especialidade em texto livre; todos passam a ser clínicos gerais.
UPDATE profissional_saude SET tipo = 'CLINICO_GERAL';

ALTER TABLE profissional_saude ALTER COLUMN tipo SET NOT NULL;
ALTER TABLE profissional_saude DROP COLUMN especialidade;

ALTER TABLE profissional_saude ADD CONSTRAINT ck_profissional_saude_tipo
    CHECK (tipo IN ('CLINICO_GERAL', 'MEDICO_ESPECIALISTA', 'CIRURGIAO'));

-- Cada coluna de área é preenchida exatamente para a sua categoria. Os valores de área não são
-- restringidos aqui para que incluir uma área no enum não exija nova migration.
ALTER TABLE profissional_saude ADD CONSTRAINT ck_profissional_saude_area_atuacao
    CHECK ((tipo = 'MEDICO_ESPECIALISTA' AND area_atuacao IS NOT NULL)
        OR (tipo <> 'MEDICO_ESPECIALISTA' AND area_atuacao IS NULL));
ALTER TABLE profissional_saude ADD CONSTRAINT ck_profissional_saude_area_cirurgica
    CHECK ((tipo = 'CIRURGIAO' AND area_cirurgica IS NOT NULL)
        OR (tipo <> 'CIRURGIAO' AND area_cirurgica IS NULL));

-- Listagem de profissionais por área.
CREATE INDEX idx_profissional_saude_area_atuacao ON profissional_saude (area_atuacao);
CREATE INDEX idx_profissional_saude_area_cirurgica ON profissional_saude (area_cirurgica);
