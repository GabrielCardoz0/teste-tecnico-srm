import { CircleCheckIcon } from "lucide-react"

import { formatDateTime } from "@/lib/datetime"
import { formatBrl, formatExchangeRate, formatUsd } from "@/lib/money"

import type { Settlement } from "./api"

export function SettlementSummary({ settlement }: { settlement: Settlement }) {
  const usd = settlement.paymentCurrency === "USD"

  return (
    <div role="status" className="flex flex-col gap-4">
      <div className="flex items-center gap-2 text-emerald-700 dark:text-emerald-400">
        <CircleCheckIcon className="size-5" />
        <p className="font-medium">Liquidação #{settlement.id} registrada</p>
      </div>

      <div className="flex flex-col gap-1 rounded-lg bg-muted/60 p-4">
        <p className="text-muted-foreground">Valor pago</p>
        <p className="font-heading text-3xl font-semibold tracking-tight tabular-nums">
          {usd
            ? formatUsd(settlement.paymentAmount)
            : formatBrl(settlement.paymentAmount)}
        </p>
        <p className="text-muted-foreground">
          Liquidado em {formatDateTime(settlement.settledAt)}
        </p>
      </div>

      <dl className="flex flex-col gap-2.5">
        <Row label="Prazo">
          {settlement.termMonths}{" "}
          {settlement.termMonths === 1 ? "mês" : "meses"}
        </Row>
        <Row label="Valor presente">
          {formatBrl(settlement.presentValueBrl)}
        </Row>
        <Row label="Deságio">{formatBrl(settlement.discountBrl)}</Row>
        {usd && settlement.exchangeRate !== undefined && (
          <Row label="Cotação usada">
            US$ 1 = R$ {formatExchangeRate(settlement.exchangeRate)}
          </Row>
        )}
      </dl>
    </div>
  )
}

function Row({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="tabular-nums">{children}</dd>
    </div>
  )
}
