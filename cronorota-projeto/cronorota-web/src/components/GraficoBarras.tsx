import {
  Bar,
  BarChart,
  CartesianGrid,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  type TooltipProps,
} from 'recharts';
import { formatarMinutos } from '../utils/data';

export interface ItemGrafico {
  rotulo: string;      // texto do eixo (curto)
  detalhe: string;     // linha secundária do tooltip, completa (ex.: "24/09/2026 · 3 roteiros")
  minutos: number;
}

// Série única na cor institucional (seção 1.2 do documento). Especificação
// das marcas: barra de no máximo 24px, ponta de 4px arredondada e base reta,
// grade horizontal em traço fino e discreto, rótulo só na maior barra - os
// demais valores ficam no tooltip e na tabela.
const COR = '#1F6F78';
const COR_HOVER = '#2B8791';
const GRADE = '#E7E5E1';
const TEXTO_EIXO = '#595959';

// Escala do eixo de valores: passos redondos e UMA unidade por gráfico -
// minutos até 3h, horas inteiras acima disso (é como o gerente lê).
function escalaEixo(maior: number): { ticks: number[]; rotulo: (v: number) => string } {
  const passos = maior >= 180
    ? [60, 120, 180, 240, 360, 480, 720, 1200, 2400, 4800]
    : [5, 10, 15, 30, 45, 60];
  const passo = passos.find((p) => p * 4 >= maior) ?? passos[passos.length - 1];
  const quantidade = Math.max(1, Math.ceil(maior / passo));
  return {
    ticks: Array.from({ length: quantidade + 1 }, (_, i) => i * passo),
    rotulo: maior >= 180 ? (v) => `${v / 60}h` : (v) => `${v} min`,
  };
}

function TooltipMinutos({ active, payload }: TooltipProps<number, string>) {
  if (!active || !payload?.length) return null;
  const item = payload[0].payload as ItemGrafico;
  // O valor em destaque, o rótulo em segundo plano.
  return (
    <div className="bg-white border border-neutral-200 rounded-md shadow-sm px-3 py-2 text-sm">
      <p className="font-semibold num">{formatarMinutos(item.minutos)}</p>
      <p className="text-xs text-grafite">{item.detalhe}</p>
    </div>
  );
}

export function GraficoBarras({
  dados,
  horizontal = false,
  altura = 260,
  onSelecionar,
  descricao,
}: {
  dados: ItemGrafico[];
  horizontal?: boolean;
  altura?: number;
  onSelecionar?: (indice: number) => void;
  descricao: string; // lida por leitores de tela
}) {
  const maior = dados.reduce((m, d) => Math.max(m, d.minutos), 0);
  const indiceMaior = dados.findIndex((d) => d.minutos === maior && maior > 0);

  // Rótulo direto só na barra de maior valor.
  const rotuloMaior = (props: { x?: number | string; y?: number | string; width?: number | string; height?: number | string; index?: number }) => {
    if (props.index !== indiceMaior) return null;
    const x = Number(props.x), y = Number(props.y), w = Number(props.width), h = Number(props.height);
    return horizontal ? (
      <text x={x + w + 6} y={y + h / 2} dominantBaseline="central" fontSize={12} fill="#1a1a1a" fontWeight={600}>
        {formatarMinutos(maior)}
      </text>
    ) : (
      <text x={x + w / 2} y={y - 6} textAnchor="middle" fontSize={12} fill="#1a1a1a" fontWeight={600}>
        {formatarMinutos(maior)}
      </text>
    );
  };

  const eixo = { tick: { fontSize: 12, fill: TEXTO_EIXO }, axisLine: false, tickLine: false };
  const escala = escalaEixo(maior);
  const eixoValores = {
    ...eixo,
    ticks: escala.ticks,
    domain: [0, escala.ticks[escala.ticks.length - 1]] as [number, number],
    tickFormatter: escala.rotulo,
    allowDecimals: false,
  };

  return (
    <div role="img" aria-label={descricao} style={{ width: '100%', height: altura }}>
      <ResponsiveContainer>
        <BarChart
          data={dados}
          layout={horizontal ? 'vertical' : 'horizontal'}
          margin={horizontal ? { top: 4, right: 72, bottom: 4, left: 8 } : { top: 24, right: 8, bottom: 4, left: 0 }}
          barCategoryGap={2}
        >
          <CartesianGrid stroke={GRADE} vertical={horizontal} horizontal={!horizontal} />
          {horizontal ? (
            <>
              <XAxis type="number" {...eixoValores} />
              <YAxis type="category" dataKey="rotulo" width={170} {...eixo}
                tickFormatter={(v: string) => (v.length > 24 ? v.slice(0, 23) + '…' : v)} />
            </>
          ) : (
            <>
              <XAxis dataKey="rotulo" {...eixo} interval="preserveStartEnd" minTickGap={16} />
              <YAxis {...eixoValores} width={60} />
            </>
          )}
          <Tooltip content={<TooltipMinutos />} cursor={{ fill: 'rgba(31,111,120,0.06)' }} />
          <Bar
            dataKey="minutos"
            fill={COR}
            maxBarSize={24}
            radius={horizontal ? [0, 4, 4, 0] : [4, 4, 0, 0]}
            activeBar={{ fill: COR_HOVER }}
            onClick={onSelecionar ? (_, indice) => onSelecionar(indice) : undefined}
            style={onSelecionar ? { cursor: 'pointer' } : undefined}
            isAnimationActive={false}
          >
            <LabelList dataKey="minutos" content={rotuloMaior} />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
