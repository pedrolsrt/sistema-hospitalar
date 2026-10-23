-- Atendimento de emergência e procedimento cirúrgico na herança JOINED de atendimento.
-- As restrições repetem as regras do domínio para que dados gravados por fora da aplicação
-- também respeitem as máquinas de estados.

CREATE TABLE atendimento_emergencia (
    id                BIGINT      PRIMARY KEY,
    prioridade        VARCHAR(20),
    data_hora_chegada TIMESTAMP   NOT NULL,
    situacao          VARCHAR(30) NOT NULL,
    CONSTRAINT fk_atendimento_emergencia_atendimento FOREIGN KEY (id) REFERENCES atendimento (id),
    CONSTRAINT ck_atendimento_emergencia_prioridade
        CHECK (prioridade IS NULL OR prioridade IN ('BAIXA', 'MODERADA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_atendimento_emergencia_situacao
        CHECK (situacao IN ('AGUARDANDO_AVALIACAO', 'EM_ATENDIMENTO', 'CONCLUIDO')),
    -- A prioridade pode faltar enquanto o paciente aguarda avaliação, mas é exigida para iniciar.
    CONSTRAINT ck_atendimento_emergencia_prioridade_definida
        CHECK (situacao = 'AGUARDANDO_AVALIACAO' OR prioridade IS NOT NULL)
);

-- Fila da emergência: atendimentos de uma situação ordenados por chegada.
CREATE INDEX idx_atendimento_emergencia_situacao ON atendimento_emergencia (situacao, data_hora_chegada);

CREATE TABLE procedimento_cirurgico (
    id                          BIGINT        PRIMARY KEY,
    descricao                   VARCHAR(500)  NOT NULL,
    tipo_cirurgia               VARCHAR(20)   NOT NULL,
    observacoes_pre_operatorias VARCHAR(2000),
    observacoes_pos_operatorias VARCHAR(2000),
    data_hora_prevista          TIMESTAMP     NOT NULL,
    data_hora_inicio            TIMESTAMP,
    data_hora_fim               TIMESTAMP,
    situacao                    VARCHAR(20)   NOT NULL,
    CONSTRAINT fk_procedimento_cirurgico_atendimento FOREIGN KEY (id) REFERENCES atendimento (id),
    CONSTRAINT ck_procedimento_cirurgico_tipo CHECK (tipo_cirurgia IN ('ELETIVA', 'URGENCIA', 'EMERGENCIA')),
    CONSTRAINT ck_procedimento_cirurgico_situacao
        CHECK (situacao IN ('AGENDADO', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO')),
    -- Início e fim acompanham a situação: só há fim depois de um início, o que impede concluir sem iniciar.
    CONSTRAINT ck_procedimento_cirurgico_datas_situacao CHECK (
        (situacao IN ('AGENDADO', 'CANCELADO') AND data_hora_inicio IS NULL AND data_hora_fim IS NULL)
        OR (situacao = 'EM_ANDAMENTO' AND data_hora_inicio IS NOT NULL AND data_hora_fim IS NULL)
        OR (situacao = 'CONCLUIDO' AND data_hora_inicio IS NOT NULL AND data_hora_fim IS NOT NULL)),
    CONSTRAINT ck_procedimento_cirurgico_fim CHECK (data_hora_fim IS NULL OR data_hora_fim >= data_hora_inicio)
);
