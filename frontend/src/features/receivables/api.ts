import { apiFetch } from "@/lib/api"
import type { ReceivableType } from "@/lib/domain"

export type CreateReceivableRequest = {
  assignorId: number
  type: ReceivableType
  faceValue: string
  dueDate: string
}

export type Receivable = {
  id: number
  assignorId: number
  type: ReceivableType
  faceValue: number
  dueDate: string
  status: "PENDING" | "SETTLED"
  createdAt: string
}

type Page<T> = {
  content: T[]
  totalElements: number
}

export function createReceivable(
  request: CreateReceivableRequest
): Promise<Receivable> {
  return apiFetch<Receivable>("/receivables", {
    method: "POST",
    body: JSON.stringify(request),
  })
}

export async function hasSimilarReceivable(
  request: CreateReceivableRequest
): Promise<boolean> {
  const query = new URLSearchParams({
    assignorId: String(request.assignorId),
    faceValue: request.faceValue,
    dueDate: request.dueDate,
    size: "1",
  })
  const page = await apiFetch<Page<Receivable>>(`/receivables?${query}`)
  return page.totalElements > 0
}
