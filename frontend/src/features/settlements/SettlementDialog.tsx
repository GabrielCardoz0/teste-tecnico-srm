import { useMemo } from "react"

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
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import type { Receivable } from "@/features/receivables/api"
import type { SimulationRequest } from "@/features/simulation/api"
import { SimulationResult } from "@/features/simulation/SimulationResult"
import { useSimulation } from "@/features/simulation/useSimulation"
import { formatDate } from "@/lib/datetime"
import {
  PAYMENT_CURRENCY_LABELS,
  RECEIVABLE_TYPE_LABELS,
  type PaymentCurrency,
} from "@/lib/domain"
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
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Liquidar recebível</AlertDialogTitle>
          {receivable && (
            <AlertDialogDescription>
              {assignorName(receivable.assignorId)} ·{" "}
              {RECEIVABLE_TYPE_LABELS[receivable.type]} ·{" "}
              {formatBrl(receivable.faceValue)} · vence em{" "}
              {formatDate(receivable.dueDate)}
            </AlertDialogDescription>
          )}
        </AlertDialogHeader>

        {state.status === "success" && (
          <SettlementSummary settlement={state.settlement} />
        )}

        {state.status === "conflict" && (
          <p role="alert" className="text-destructive">
            Este recebível já foi liquidado.
          </p>
        )}

        {!finished && (
          <div className="flex flex-col gap-4 text-sm">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="settlementCurrency">Moeda de pagamento</Label>
              <Select
                id="settlementCurrency"
                items={PAYMENT_CURRENCY_LABELS}
                value={currency}
                onValueChange={(value) =>
                  value && onCurrencyChange(value as PaymentCurrency)
                }
                disabled={submitting}
              >
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(PAYMENT_CURRENCY_LABELS).map(
                    ([value, label]) => (
                      <SelectItem key={value} value={value}>
                        {label}
                      </SelectItem>
                    )
                  )}
                </SelectContent>
              </Select>
            </div>

            <div aria-live="polite">
              <SimulationResult state={simulation} />
            </div>

            {(state.status === "rejected" || state.status === "failed") && (
              <div
                role="alert"
                className="flex flex-col gap-1 text-destructive"
              >
                <p className="font-medium">
                  {state.status === "failed"
                    ? "Não foi possível concluir a liquidação. Tente novamente."
                    : "Não foi possível liquidar"}
                </p>
                {state.problem.detail && <p>{state.problem.detail}</p>}
              </div>
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
