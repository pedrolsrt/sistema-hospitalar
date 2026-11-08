-- Encaminhamento do paciente para atendimento especializado, registrado na consulta de rotina.
-- Nulo quando não houve encaminhamento, o que vale para todas as consultas existentes.
ALTER TABLE consulta_rotina ADD COLUMN area_encaminhamento VARCHAR(30);
