-- Tipo do quarto. Os quartos existentes passam a ser enfermarias, cujo limite é de 6 leitos.

-- Interrompe a migração se algum quarto não couber em uma enfermaria. Não há RAISE portável entre
-- PostgreSQL e H2; converter o texto da mensagem para inteiro falha só quando há linhas, e o erro do
-- banco mostra o texto. A mensagem depende da linha para que o banco não a avalie antecipadamente.
SELECT CAST('Migração V6 interrompida: o quarto ' || numero || ' tem capacidade ' || capacidade_maxima
            || ', acima do limite de 6 leitos da enfermaria. Ajuste a capacidade antes de migrar.' AS INTEGER)
FROM quarto
WHERE capacidade_maxima > 6;

ALTER TABLE quarto ADD COLUMN tipo VARCHAR(20);
UPDATE quarto SET tipo = 'ENFERMARIA';
ALTER TABLE quarto ALTER COLUMN tipo SET NOT NULL;

-- Só os valores aceitos; o limite de capacidade por tipo fica no enum TipoQuarto, porque é uma
-- suposição do grupo que pode mudar sem exigir migration.
ALTER TABLE quarto ADD CONSTRAINT ck_quarto_tipo CHECK (tipo IN ('ENFERMARIA', 'APARTAMENTO', 'UTI'));

CREATE INDEX idx_quarto_tipo ON quarto (tipo);
