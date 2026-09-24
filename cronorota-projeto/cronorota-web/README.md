# CronoRota Web

Front-end do MVP. React 18 + TypeScript + Vite + Tailwind CSS + Recharts (gráficos do dashboard).

## Como rodar

Pré-requisito: a API precisa estar rodando (ver o README de `cronorota-api`),
escutando em `http://localhost:8080`.

```bash
npm install
npm run dev
```

Abre em `http://localhost:5173`.

## Como testar o fluxo completo

1. Criar o primeiro administrador em `/bootstrap-admin` (só em desenvolvimento)
2. Entrar como administrador: cadastrar um gerente e um parâmetro de custo
   (sem parâmetro vigente, o custo estimado não aparece)
3. Entrar como gerente: cadastrar um motorista (ele entra na equipe desse
   gerente), alguns endereços e pedidos com data prevista para o dia do roteiro
4. Montar um roteiro, clicando nos pedidos na ordem do trajeto - o primeiro
   clicado vira o ponto de partida (RN01)
5. Entrar como o motorista (ou, como gerente, usar "Ver como o motorista
   veria"): na partida registra-se só a saída; nos demais pontos, chegada e
   saída, e o tempo parado é calculado a cada saída (RN01-RN04)
6. Como gerente, abrir o Dashboard (tela inicial dele) e o Histórico. No
   dashboard, clicar numa barra do gráfico por dia/mês abre o histórico
   daquele recorte; no gráfico por período, abre o roteiro. O motorista tem
   o próprio histórico pelo link no topo de "Meus roteiros".
7. No histórico, "Exportar CSV/PDF" baixa o mesmo conteúdo da tela (UC14).

Pra testar de verdade a experiência mobile, abre essa URL do motorista no
DevTools do navegador com o modo de emulação de celular ligado (F12 →
ícone de celular/tablet no canto superior).

## O que esta interface cobre (e o que não cobre ainda)

Cobre as telas ligadas aos casos de uso já implementados no back-end: UC01
(login), UC02, UC03, UC04, UC05, UC06, UC07/UC08, UC09 (histórico), UC10
(dashboard), UC11/UC12 (parâmetros) e UC14 (exportação). Não inclui ainda
o modo offline (RNF07) - fica pro próximo incremento.
