import { useMemo } from "react"

import { CurrencyToggle } from "@/components/CurrencyToggle"
import { ProblemAlert } from "@/components/ProblemAlert"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Badge } from "@/components/ui/badge"
import { Label } from "@/components/ui/label"
import type { Receivable } from "@/features/receivables/api"
import type { SimulationRequest } from "@/features/simulation/api"
import { SimulationResult } from "@/features/simulation/SimulationResult"
import { useSimulation } from "@/features/simulation/useSimulation"
import { formatDate } from "@/lib/datetime"
import { RECEIVABLE_TYPE_LABELS, type PaymentCurrency } from "@/lib/domain"
import { formatBrl } from "@/lib/money"

import { SettlementSummary } from "./SettlementSummary"
import type { SettlementStatus } from "./useSettlement"

type SettlementDialogProps = {
  receivable: Receivable | null
  assignorName: (assignorId: number) => string
  currency: PaymentCurrency
  state: SettlementStatus
  onCurrencyChange: (currency: PaymentCurrency) => void
  onConfirm: () => void
  onClose: () => void
}

export function SettlementDialog({
  receivable,
  assignorName,
  currency,
  state,
  onCurrencyChange,
  onConfirm,
  onClose,
}: SettlementDialogProps) {
  const finished = state.status === "success" || state.status === "conflict"
  const submitting = state.status === "submitting"

  const simulationRequest = useMemo<SimulationRequest | null>(
    () =>
      receivable && !finished
        ? {
            type: receivable.type,
            faceValue: String(receivable.faceValue),
            dueDate: receivable.dueDate,
            paymentCurrency: currency,
          }
        : null,
    [receivable, finished, currency]
  )
  const simulation = useSimulation(simulationRequest)

  return (
    <AlertDialog
      open={receivable !== null}
      onOpenChange={(open) => !open && !submitting && onClose()}
    >
      <AlertDialogContent className="data-[size=default]:max-w-[calc(100%-2rem)] data-[size=default]:sm:max-w-lg">
        <AlertDialogHeader>
          <AlertDialogTitle>Liquidar recebível</AlertDialogTitle>
          <AlertDialogDescription>
            Confira o valor na moeda escolhida antes de confirmar.
          </AlertDialogDescription>
        </AlertDialogHeader>

        {receivable && (
          <dl className="grid grid-cols-2 gap-x-6 gap-y-3 rounded-lg border p-4 text-sm">
            <div className="col-span-2 flex flex-col gap-0.5">
              <dt className="text-xs text-muted-foreground">Cedente</dt>
              <dd className="font-medium">
                {assignorName(receivable.assignorId)}
              </dd>
            </div>
            <div className="flex flex-col gap-1">
              <dt className="text-xs text-muted-foreground">Tipo</dt>
              <dd>
                <Badge variant="secondary">
                  {RECEIVABLE_TYPE_LABELS[receivable.type]}
                </Badge>
              </dd>
            </div>
            <div className="flex flex-col gap-0.5">
              <dt className="text-xs text-muted-foreground">Vencimento</dt>
              <dd className="tabular-nums">{formatDate(receivable.dueDate)}</dd>
            </div>
            <div className="col-span-2 flex flex-col gap-0.5">
              <dt className="text-xs text-muted-foreground">Valor de face</dt>
              <dd className="tabular-nums">
                {formatBrl(receivable.faceValue)}
              </dd>
            </div>
          </dl>
        )}

        {state.status === "success" && (
          <SettlementSummary settlement={state.settlement} />
        )}

        {state.status === "conflict" && (
          <ProblemAlert
            title="Este recebível já foi liquidado"
            problem={{
              ...state.problem,
              detail: "A lista será atualizada ao fechar.",
            }}
          />
        )}

        {!finished && (
          <div className="flex flex-col gap-5 text-sm">
            <div className="flex flex-col gap-2">
              <Label id="settlementCurrency-label">Moeda de pagamento</Label>
              <CurrencyToggle
                value={currency}
                onChange={onCurrencyChange}
                disabled={submitting}
                labelledBy="settlementCurrency-label"
              />
            </div>

            <div aria-live="polite">
              <SimulationResult state={simulation} />
            </div>

            {state.status === "failed" && (
              <ProblemAlert
                title="Não foi possível concluir a liquidação"
                problem={{
                  ...state.problem,
                  detail:
                    "Verifique a conexão e tente novamente. O reenvio é seguro: a liquidação não será duplicada.",
                }}
              />
            )}
            {state.status === "rejected" && (
              <ProblemAlert
                title="Não foi possível liquidar"
                problem={state.problem}
              />
            )}
          </div>
        )}

        <AlertDialogFooter>
          {finished ? (
            <AlertDialogAction onClick={onClose}>Fechar</AlertDialogAction>
          ) : (
            <>
              <AlertDialogCancel disabled={submitting}>
                Cancelar
              </AlertDialogCancel>
              <AlertDialogAction onClick={onConfirm} disabled={submitting}>
                {submitting
                  ? "Liquidando..."
                  : state.status === "failed"
                    ? "Tentar novamente"
                    : "Confirmar liquidação"}
              </AlertDialogAction>
            </>
          )}
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
