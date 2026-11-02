import { CalculatorIcon } from "lucide-react"

import { ProblemAlert } from "@/components/ProblemAlert"
import { Skeleton } from "@/components/ui/skeleton"
import { formatDateTime } from "@/lib/datetime"
import {
  formatBrl,
  formatExchangeRate,
  formatRate,
  formatUsd,
} from "@/lib/money"

import type { SimulationState } from "./useSimulation"

export function SimulationResult({
  state,
  idleHint,
}: {
  state: SimulationState
  idleHint?: string
}) {
  if (state.status === "idle") {
    return (
      <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed px-6 py-10 text-center">
        <div className="flex size-10 items-center justify-center rounded-full bg-muted">
          <CalculatorIcon className="size-5 text-muted-foreground" />
        </div>
        <div className="flex flex-col gap-1">
          <p className="font-medium">
            {idleHint ? "Revise os dados" : "Nada para simular ainda"}
          </p>
          <p className="text-muted-foreground">
            {idleHint ??
              "Preencha tipo, valor de face e vencimento para ver o valor líquido."}
          </p>
        </div>
      </div>
    )
  }

  if (state.status === "loading") {
    return (
      <div className="flex flex-col gap-5" aria-busy="true">
        <div className="flex flex-col gap-2">
          <Skeleton className="h-4 w-36" />
          <Skeleton className="h-9 w-52" />
          <Skeleton className="h-4 w-40" />
        </div>
        <div className="flex flex-col gap-3 border-t pt-4">
          {[0, 1, 2, 3].map((row) => (
            <div key={row} className="flex justify-between">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-4 w-20" />
            </div>
          ))}
        </div>
      </div>
    )
  }

  if (state.status === "error") {
    return (
      <ProblemAlert
        title={
          state.problem.status === 422 ? "Não foi possível simular" : undefined
        }
        problem={state.problem}
      />
    )
  }

  const { result } = state
  const usd =
    result.exchangeRate !== undefined && result.amountUsd !== undefined

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-1">
        <p className="text-muted-foreground">
          {usd ? "Valor líquido em dólar" : "Valor líquido a receber"}
        </p>
        <p className="font-heading text-3xl font-semibold tracking-tight tabular-nums">
          {usd && result.amountUsd !== undefined
            ? formatUsd(result.amountUsd)
            : formatBrl(result.presentValueBrl)}
        </p>
        <p className="text-muted-foreground">
          {usd
            ? `Equivale a ${formatBrl(result.presentValueBrl)} (valor presente)`
            : `Deságio de ${formatBrl(result.discountBrl)}`}
        </p>
      </div>

      <dl className="flex flex-col gap-2.5 border-t pt-4">
        <Row label="Prazo">
          {result.termMonths} {result.termMonths === 1 ? "mês" : "meses"}
        </Row>
        <Row label="Taxa base">{formatRate(result.baseRate)} a.m.</Row>
        <Row label="Spread">{formatRate(result.spread)} a.m.</Row>
        <Row label="Valor presente">{formatBrl(result.presentValueBrl)}</Row>
        <Row label="Deságio">{formatBrl(result.discountBrl)}</Row>
        {result.exchangeRate && (
          <Row
            label="Cotação usada"
            hint={`vigente desde ${formatDateTime(result.exchangeRate.validFrom)}`}
          >
            US$ 1 = R$ {formatExchangeRate(result.exchangeRate.rate)}
          </Row>
        )}
      </dl>
    </div>
  )
}

function Row({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: React.ReactNode
}) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="text-right tabular-nums">
        {children}
        {hint && (
          <span className="block text-xs text-muted-foreground">{hint}</span>
        )}
      </dd>
    </div>
  )
}
