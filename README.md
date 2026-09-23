# Sistema de Informação Hospitalar

API REST para gestão hospitalar: pacientes, profissionais de saúde, consultas, internações,
quartos e histórico médico. Trabalho prático da disciplina de Programação Modular (PUC Minas).

## Stack

- Java 21 e Spring Boot 4 (Web MVC, Data JPA, Validation)
- PostgreSQL 17 instalado localmente
- Flyway para migrations (schema validado pelo Hibernate com `ddl-auto=validate`)
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, AssertJ, H2 (modo PostgreSQL) e JaCoCo
- Maven Wrapper

## Pré-requisitos

- Java 21 (JDK Temurin recomendado)
- PostgreSQL 17 (instalador oficial para Windows)

Não é necessário instalar o Maven: o projeto usa o Maven Wrapper (`mvnw.cmd`).

## Criando o banco de dados

Com o PostgreSQL em execução, crie o banco `hospital` (no PowerShell):

```powershell
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -h localhost -U postgres -c "CREATE DATABASE hospital"
```

As tabelas são criadas automaticamente pelo Flyway na primeira execução da aplicação.

## Configuração da conexão

A conexão é lida de variáveis de ambiente, com valores padrão para o ambiente local:

| Variável      | Padrão                                     |
|---------------|--------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/hospital` |
| `DB_USER`     | `postgres`                                 |
| `DB_PASSWORD` | `hospital123`                              |

Para usar outros valores, defina as variáveis antes de iniciar a aplicação:

```powershell
$env:DB_PASSWORD = "minha-senha"
```

## Como rodar no Windows (PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

## Documentação da API

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI (JSON): http://localhost:8080/v3/api-docs

## Testes e cobertura

Os testes usam o perfil `test`, com banco H2 em memória no modo PostgreSQL. Não é preciso
ter o PostgreSQL rodando para executá-los.

```powershell
.\mvnw.cmd verify
```

O relatório de cobertura do JaCoCo é gerado em `target/site/jacoco/index.html`.
