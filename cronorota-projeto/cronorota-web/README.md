# CronoRota Web

Front-end do MVP. React 18 + TypeScript + Vite + Tailwind CSS.

## Como rodar

Pré-requisito: a API precisa estar rodando (`docker compose up -d --build`
na pasta `cronorota-api`), escutando em `http://localhost:8080`.

```bash
npm install
npm run dev
```

Abre em `http://localhost:5173`.

## Como testar o fluxo completo

A página inicial (`/gerente`) lista a ordem certa de teste:

1. Cadastrar um gerente
2. Cadastrar um motorista (com veículo)
3. Cadastrar alguns endereços
4. Cadastrar pedidos nesses endereços
5. Montar um roteiro, clicando nos pedidos na ordem do trajeto - o primeiro
   clicado vira o ponto de partida (RN01)
6. Na tela do roteiro, clicar em "Ver como o motorista veria" - isso abre a
   visão mobile (`/motorista/roteiro/:id`), onde dá pra registrar chegada e
   saída em cada ponto e ver o tempo parado sendo calculado automaticamente
   (RN01-RN04) a cada saída registrada

Pra testar de verdade a experiência mobile, abre essa URL do motorista no
DevTools do navegador com o modo de emulação de celular ligado (F12 →
ícone de celular/tablet no canto superior).

## O que esta interface cobre (e o que não cobre ainda)

Cobre as telas ligadas aos casos de uso já implementados no back-end: UC02,
UC03, UC04, UC05, UC06, UC07/UC08. Não inclui ainda: tela de login (UC01 -
hoje não tem autenticação, então não tem tela de login), histórico (UC09),
dashboard com gráficos (UC10), parametrização de custos (UC11/UC12) e
exportação de relatório (UC14) - ficam pros próximos incrementos, junto
com o back-end correspondente.
