// Estes tipos espelham, campo a campo, os DTOs de response do back-end
// (com.cronorota.dto.response) - é o "contrato" entre front e back. Se um
// campo mudar de nome lá no Java, precisa mudar aqui também (é justamente
// o tipo de coisa que o springdoc-openapi, mencionado no documento, ajudaria
// a manter sincronizado automaticamente numa próxima etapa).

export interface Gerente {
  id: number;
  nome: string;
  email: string;
}

export interface Endereco {
  id: number;
  logradouro: string;
  bairro: string;
  cidade: string;
  uf: string;
  cep: string;
}

export interface Pedido {
  id: number;
  codigo: string;
  destinatario: string;
  endereco: Endereco;
  dataPrevista: string;
  situacao: 'PENDENTE' | 'EM_ROTEIRO' | 'ENTREGUE' | 'CANCELADO';
}

export interface Motorista {
  id: number;
  nome: string;
  telefone: string;
  email: string;
  documento: string;
  placaVeiculo: string;
  gerenteId: number;
  ativo: boolean;
}

export interface Ponto {
  id: number;
  ordem: number;
  endereco: string;
  dataHoraChegada: string | null;
  dataHoraSaida: string | null;
  tempoParadoMinutos: number | null;
}

export interface Roteiro {
  id: number;
  data: string;
  motorista: string;
  tempoTotalParadoMinutos: number | null;
  percentualJornada: number | null;
  custoEstimado: number | null;
  pontos: Ponto[];
}

export interface Parametro {
  id: number;
  valorCombustivel: number;
  jornadaPadraoMinutos: number;
  dataInicioVigencia: string;
  dataFimVigencia: string | null;
}

export interface ErroApi {
  timestamp: string;
  status: number;
  mensagem: string;
}

// UC09 - GET /historico
export interface LinhaHistorico {
  roteiroId: number;
  data: string;
  motoristaId: number;
  motorista: string;
  ordem: number;
  endereco: string;
  dataHoraChegada: string;
  dataHoraSaida: string;
  tempoParadoMinutos: number;
}

export interface Historico {
  inicio: string;
  fim: string;
  quantidadeRoteiros: number;
  quantidadePontos: number;
  tempoTotalParadoMinutos: number;
  mediaPorRoteiroMinutos: number;
  mediaPorPontoMinutos: number;
  pontos: LinhaHistorico[];
}

// UC10 - GET /dashboard
export interface TotalNoIntervalo {
  chave: string;
  tempoParadoMinutos: number;
  quantidadeRoteiros: number;
}

export interface Dashboard {
  inicio: string;
  fim: string;
  indicadores: {
    quantidadeRoteiros: number;
    tempoTotalParadoMinutos: number;
    percentualJornadaMedio: number | null;
    pontoMaisCritico: {
      roteiroId: number;
      data: string;
      motorista: string;
      endereco: string;
      tempoParadoMinutos: number;
    } | null;
    custoEstimadoTotal: number | null;
    custoCompleto: boolean;
  };
  porDia: TotalNoIntervalo[];
  porMes: TotalNoIntervalo[];
  porRoteiro: {
    roteiroId: number;
    data: string;
    motorista: string;
    tempoParadoMinutos: number;
    percentualJornada: number | null;
  }[];
  rankingEnderecos: { enderecoId: number; endereco: string; tempoParadoMinutos: number; paradas: number }[];
  porMotorista: {
    motoristaId: number;
    motorista: string;
    quantidadeRoteiros: number;
    tempoParadoMinutos: number;
    mediaPorRoteiroMinutos: number;
  }[];
}

// RNF05 - GET /auditoria/alteracoes e /roteiros/:id/alteracoes
export interface Alteracao {
  revisao: number;
  instante: string;
  login: string;
  perfil: string;
  entidade: string;
  entidadeNome: string;
  registroId: number;
  operacao: 'INCLUSAO' | 'ALTERACAO' | 'REMOCAO';
  campos: { campo: string; anterior: string | null; novo: string | null }[];
}

// UC14 passo 5 - GET /auditoria/exportacoes
export interface RegistroExportacao {
  id: number;
  instante: string;
  login: string;
  perfil: string;
  formato: string;
  periodoInicio: string;
  periodoFim: string;
  motoristaId: number | null;
  quantidadeLinhas: number;
}
