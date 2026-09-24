import type { ReactNode } from 'react';

// Componentes bem pequenos e sem estado, reaproveitados nas páginas.
// Mantê-los aqui evita repetir as mesmas classes Tailwind em toda tela.

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <div className={`bg-white rounded-lg border border-neutral-200 p-6 ${className}`}>
      {children}
    </div>
  );
}

export function Button({
  children,
  variant = 'primary',
  className = '',
  ...props
}: {
  children: ReactNode;
  variant?: 'primary' | 'secondary';
} & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  const base = 'px-4 py-2.5 rounded-md font-medium text-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed';
  const styles =
    variant === 'primary'
      ? 'bg-petroleo text-white hover:bg-[#175a61]'
      : 'bg-white text-petroleo border border-petroleo hover:bg-petroleo/5';
  // className vem DEPOIS de base/styles na string final, pra complementar
  // (ex.: "w-full") em vez de apagar as classes de cor/padding do botão -
  // {...props} não inclui mais className porque já foi desestruturado acima.
  return (
    <button className={`${base} ${styles} ${className}`} {...props}>
      {children}
    </button>
  );
}

export function Field({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <label className="block">
      <span className="block text-sm font-medium text-grafite mb-1.5">{label}</span>
      {children}
    </label>
  );
}

const inputBase =
  'w-full rounded-md border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-petroleo/40 focus:border-petroleo';

export function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input className={inputBase} {...props} />;
}

export function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={inputBase} {...props} />;
}

export function Alert({ tipo, children }: { tipo: 'erro' | 'sucesso'; children: ReactNode }) {
  const styles =
    tipo === 'erro'
      ? 'bg-terracota/10 text-terracota border-terracota/30'
      : 'bg-verdeok/10 text-verdeok border-verdeok/30';
  return <div className={`rounded-md border px-4 py-3 text-sm ${styles}`}>{children}</div>;
}
