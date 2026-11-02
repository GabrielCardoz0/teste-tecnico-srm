import { apiFetch, type Page } from "@/lib/api"
import type { PaymentCurrency, ReceivableType } from "@/lib/domain"

export type StatementLine = {
  settlementId: number
  settledAt: string
  assignorLegalName: string
  assignorCnpj: string
  receivableType: ReceivableType
  faceValue: number
  presentValueBrl: number
  discountBrl: number
  paymentCurrency: PaymentCurrency
  exchangeRate?: number
  paymentAmount: number
}

export type StatementQuery = {
  from?: string
  to?: string
  assignorId?: number
  paymentCurrency?: PaymentCurrency
  page: number
  size: number
}

export function listSettlements(
  query: StatementQuery,
  signal: AbortSignal
): Promise<Page<StatementLine>> {
  const params = new URLSearchParams()
  if (query.from) {
    params.set("from", query.from)
  }
  if (query.to) {
    params.set("to", query.to)
  }
  if (query.assignorId !== undefined) {
    params.set("assignorId", String(query.assignorId))
  }
  if (query.paymentCurrency) {
    params.set("paymentCurrency", query.paymentCurrency)
  }
  params.set("page", String(query.page))
  params.set("size", String(query.size))
  return apiFetch<Page<StatementLine>>(`/settlements?${params}`, { signal })
}
