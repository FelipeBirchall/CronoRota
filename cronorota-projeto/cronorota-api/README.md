# CronoRota API

Back-end do MVP (Engenharia de Software II, PUC Minas). Java 21 + Spring Boot 3.3 + PostgreSQL 16.

## Como rodar

```bash
cp .env.example .env   # só na primeira vez - ajuste senha e JWT_SECRET
docker compose up -d --build
```

Isso sobe dois contêineres: `postgres` (banco) e `api` (a aplicação Java, compilada
dentro do próprio Docker pelo `Dockerfile`). Na primeira vez, o Flyway roda a
migrações de `src/main/resources/db/migration` automaticamente.

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
├── auditoria/    trilha de auditoria (Envers): revisão com autor, leitura
│                 da trilha, registro de exportações
├── relatorio/    geração do relatório do período em CSV e PDF
├── security/     JWT (geração/validação do token) e o usuário autenticado
└── config/       configuração do Spring Security (rotas por perfil, CORS, BCrypt)
```

## Testes

```bash
mvn test
```

Testes unitários (JUnit 5 + Mockito, sem banco) das regras de cálculo e de
montagem/registro: `TempoParadoService` e `CustoService` usam os exemplos da
seção 14 do documento (roteiros A, B e C; R$ 24,00 de custo), e os demais
cobrem RN05, RN12, RN13, RN14, os fluxos de exceção do UC07 e a agregação
do histórico e do dashboard (`AgregacaoTest`, também com os roteiros A/B/C)
e a exportação (`RelatorioTest` lê o CSV e o texto do PDF gerados), além do
autor da revisão e da comparação de versões da auditoria (`AuditoriaTest`).

## O que este código cobre (e o que não cobre ainda)

**Implementado:**
- UC01 (login com JWT) e RN13/RNF04 (acesso por perfil): o perfil é checado
  por rota no `SecurityConfig`, e o dono do registro (motorista do roteiro,
  gerente da equipe) é checado nos services - ex.: `RoteiroService.verificarAcesso`
- UC02 (motorista, na equipe do gerente logado), UC03 (gerente, versão
  simplificada), UC04 (endereço, sem geocodificação), UC05 (pedidos)
- UC06 (montar roteiro): RN05 (um roteiro por motorista/data), RN12
  (só pedidos pendentes da data), motorista ativo e da própria equipe;
  pedidos no mesmo endereço viram um ponto só
- UC07 (chegada/saída): partida só registra saída (A1), saída não pode ser
  anterior à chegada (E1), sem registro duplicado; UC08 (tempo parado - RN01
  a RN04) e UC13 (custo - RN07)
- UC11/UC12 (parâmetros) com RN14: nova vigência encerra a anterior, não
  aceita sobreposição e recalcula os roteiros alcançados
- UC09 (`GET /api/historico?inicio=&fim=&motoristaId=`): paradas concluídas
  do período com endereço e horários, mais os totalizadores (total, média
  por roteiro, média por ponto), filtradas por perfil (RN13)
- UC10 (`GET /api/dashboard?inicio=&fim=&motoristaId=`, gerente e admin):
  indicadores (tempo total, % da jornada, ponto mais crítico, custo
  estimado), totais por dia, por mês e por roteiro, ranking de endereços e
  comparação entre motoristas - tudo calculado a partir do mesmo conjunto
  de roteiros, carregado numa consulta só
- UC14 (`GET /api/historico/exportar?inicio=&fim=&motoristaId=&formato=csv|pdf&maiorTempo=`):
  o mesmo conteúdo do histórico (e na mesma ordenação da tela) como arquivo,
  com período, filtro e data/hora de geração no cabeçalho. CSV pronto para o
  Excel em português (";", vírgula decimal, UTF-8 com BOM, proteção contra
  fórmula) e PDF em A4 paisagem (OpenPDF). Acima de 5.000 paradas pede para
  reduzir o período (E1). Horários sempre em America/Sao_Paulo. Cada
  exportação fica registrada na auditoria (passo 5)
- RNF05 (auditoria) com Hibernate Envers: toda inclusão, alteração ou
  remoção de ponto, roteiro, parâmetro, pedido, usuário, veículo e endereço
  gera uma versão em `<tabela>_aud`, ligada a uma `revisao` com o usuário
  (lido do token) e a data/hora - sem nenhum service precisar lembrar disso.
  A trilha é somente para inclusão (o banco recusa UPDATE/DELETE, seção
  28.2) e o hash da senha não é versionado. Consulta:
  `GET /api/auditoria/alteracoes` e `/api/auditoria/exportacoes` (admin) e
  `GET /api/roteiros/{id}/alteracoes` (quem pode ver o roteiro, RN13)

**Não implementado nesta etapa** (próximos incrementos):
- RN08 (bloqueio após 3 tentativas de login) e recuperação de senha
- Cache do dashboard em Redis (RNF03 - por ora a agregação em memória
  responde em milissegundos no volume do piloto)
- UC14-A1/A2 (exportar os gráficos do dashboard em PDF; enviar por e-mail)
- UC07-E2 (alerta de chegada fora da sequência) e A3 (ajuste manual com justificativa)
- Geocodificação e coordenadas obrigatórias (RN11)
- Auditoria (RNF05, a entidade `RegistroAuditoria` do documento) - ficaria
  bem resolvida com Hibernate Envers, como a seção 25.1 do documento sugere
- Modo offline (RNF07) - é responsabilidade do front-end (Workbox + Dexie.js),
  não deste back-end
