import { formatDateTime } from "@/lib/datetime"
import { formatBrl, formatExchangeRate, formatUsd } from "@/lib/money"

import type { Settlement } from "./api"

export function SettlementSummary({ settlement }: { settlement: Settlement }) {
  const usd = settlement.paymentCurrency === "USD"

  return (
    <div role="status" className="flex flex-col gap-3">
      <p className="font-medium text-emerald-700 dark:text-emerald-400">
        Liquidação #{settlement.id} registrada.
      </p>
      <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2">
        <dt className="text-muted-foreground">Valor presente</dt>
        <dd>{formatBrl(settlement.presentValueBrl)}</dd>

        <dt className="text-muted-foreground">Deságio</dt>
        <dd>{formatBrl(settlement.discountBrl)}</dd>

        {usd && settlement.exchangeRate !== undefined && (
          <>
            <dt className="text-muted-foreground">Cotação usada</dt>
            <dd>US$ 1 = R$ {formatExchangeRate(settlement.exchangeRate)}</dd>
          </>
        )}

        <dt className="text-muted-foreground">Valor pago</dt>
        <dd className="font-medium">
          {usd
            ? formatUsd(settlement.paymentAmount)
            : formatBrl(settlement.paymentAmount)}
        </dd>

        <dt className="text-muted-foreground">Liquidado em</dt>
        <dd>{formatDateTime(settlement.settledAt)}</dd>
      </dl>
    </div>
  )
}
