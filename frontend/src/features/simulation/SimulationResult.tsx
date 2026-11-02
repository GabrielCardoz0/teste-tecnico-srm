import { formatDateTime } from "@/lib/datetime"
import {
  formatBrl,
  formatExchangeRate,
  formatRate,
  formatUsd,
} from "@/lib/money"

import type { SimulationState } from "./useSimulation"

export function SimulationResult({ state }: { state: SimulationState }) {
  if (state.status === "idle") {
    return (
      <p className="text-muted-foreground">
        Preencha tipo, valor de face, vencimento e moeda para simular.
      </p>
    )
  }

  if (state.status === "loading") {
    return <p className="text-muted-foreground">Calculando...</p>
  }

  if (state.status === "error") {
    const { problem } = state
    return (
      <div role="alert" className="flex flex-col gap-1 text-destructive">
        <p className="font-medium">
          {problem.status === 422 ? "Não foi possível simular" : problem.title}
        </p>
        {problem.detail && <p>{problem.detail}</p>}
      </div>
    )
  }

  const { result } = state
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2">
      <dt className="text-muted-foreground">Prazo</dt>
      <dd>
        {result.termMonths} {result.termMonths === 1 ? "mês" : "meses"}
      </dd>

      <dt className="text-muted-foreground">Taxa base + spread</dt>
      <dd>
        {formatRate(result.baseRate)} + {formatRate(result.spread)} a.m.
      </dd>

      <dt className="text-muted-foreground">Valor presente</dt>
      <dd className="font-medium">{formatBrl(result.presentValueBrl)}</dd>

      <dt className="text-muted-foreground">Deságio</dt>
      <dd>{formatBrl(result.discountBrl)}</dd>

      {result.exchangeRate && result.amountUsd !== undefined && (
        <>
          <dt className="text-muted-foreground">Cotação usada</dt>
          <dd>
            US$ 1 = R$ {formatExchangeRate(result.exchangeRate.rate)}{" "}
            <span className="text-muted-foreground">
              (vigente desde {formatDateTime(result.exchangeRate.validFrom)})
            </span>
          </dd>

          <dt className="text-muted-foreground">Valor em dólar</dt>
          <dd className="font-medium">{formatUsd(result.amountUsd)}</dd>
        </>
      )}
    </dl>
  )
}
