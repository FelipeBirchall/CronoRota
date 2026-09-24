# CronoRota API

Back-end do MVP (Engenharia de Software II, PUC Minas). Java 21 + Spring Boot 3.3 + PostgreSQL 16.

## Como rodar

```bash
docker compose up -d --build
```

Isso sobe dois contêineres: `postgres` (banco) e `api` (a aplicação Java, compilada
dentro do próprio Docker pelo `Dockerfile`). Na primeira vez, o Flyway roda a
migração `V1__schema_inicial.sql` automaticamente e cria todas as tabelas.

A API fica disponível em `http://localhost:8080`.

## Estrutura de pacotes

```
com.cronorota
├── model/        entidades JPA (o "M" do MVC) - uma classe por tabela
├── repository/   interfaces Spring Data - acesso ao banco, sem SQL escrito à mão
├── service/      regras de negócio (RN01-RN14) - onde a lógica de verdade mora
├── controller/   endpoints REST - só traduz HTTP <-> DTO, chama o service
├── dto/          formato de entrada (request) e saída (response) da API,
│                 separado das entidades
├── exception/    exceções de negócio + tradutor global pra respostas HTTP
└── config/       configuração do Spring (por enquanto, só o PasswordEncoder)
```

## O que este código cobre (e o que não cobre ainda)

**Implementado:**
- UC02 (cadastrar motorista), UC06 (montar roteiro), UC07 (registrar
  chegada/saída), UC08 (calcular tempo parado - RN01 a RN04), UC13
  (calcular custo - RN07)
- Schema completo do banco (todas as 11 entidades do diagrama conceitual)

**Não implementado nesta etapa** (próximos incrementos):
- UC01 (autenticação JWT) e RNF04 (controle de acesso por perfil) - hoje
  todos os endpoints estão abertos, ver o comentário em `SecurityConfig.java`
- UC09 (histórico), UC10 (dashboard), UC14 (exportação de relatório)
- UC03, UC04, UC05 (cadastro de gerente, pontos/endereços, pedidos) - o
  `RoteiroService` assume que pedidos e endereços já existem no banco
- UC11/UC12 (telas de parametrização - a entidade `Parametro` existe e é
  usada no cálculo, mas não tem endpoint de cadastro ainda)
- Auditoria (RNF05, a entidade `RegistroAuditoria` do documento) - ficaria
  bem resolvida com Hibernate Envers, como a seção 25.1 do documento sugere
- Modo offline (RNF07) - é responsabilidade do front-end (Workbox + Dexie.js),
  não deste back-end
