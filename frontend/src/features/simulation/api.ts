import { apiFetch } from "@/lib/api"
import type { PaymentCurrency, ReceivableType } from "@/lib/domain"

export type SimulationRequest = {
  type: ReceivableType
  faceValue: string
  dueDate: string
  paymentCurrency: PaymentCurrency
}

export type SimulationResponse = {
  termMonths: number
  baseRate: number
  spread: number
  presentValueBrl: number
  discountBrl: number
  paymentCurrency: PaymentCurrency
  exchangeRate?: {
    id: number
    rate: number
    validFrom: string
  }
  amountUsd?: number
}

export function simulate(
  request: SimulationRequest,
  signal: AbortSignal
): Promise<SimulationResponse> {
  return apiFetch<SimulationResponse>("/pricing/simulations", {
    method: "POST",
    body: JSON.stringify(request),
    signal,
  })
}
