import { apiFetch } from "@/lib/api"
import type { PaymentCurrency, ReceivableType } from "@/lib/domain"

export type SettleRequest = {
  receivableId: number
  paymentCurrency: PaymentCurrency
}

export type Settlement = {
  id: number
  idempotencyKey: string
  receivableId: number
  receivableType: ReceivableType
  faceValue: number
  termMonths: number
  baseRate: number
  spread: number
  presentValueBrl: number
  discountBrl: number
  paymentCurrency: PaymentCurrency
  exchangeRateId?: number
  exchangeRate?: number
  paymentAmount: number
  settledAt: string
  createdAt: string
}

export function settle(
  request: SettleRequest,
  idempotencyKey: string
): Promise<Settlement> {
  return apiFetch<Settlement>("/settlements", {
    method: "POST",
    headers: { "Idempotency-Key": idempotencyKey },
    body: JSON.stringify(request),
  })
}
