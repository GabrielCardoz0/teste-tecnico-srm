import { apiFetch } from "@/lib/api"

export type Assignor = {
  id: number
  cnpj: string
  legalName: string
}

export function listAssignors(signal?: AbortSignal): Promise<Assignor[]> {
  return apiFetch<Assignor[]>("/assignors", { signal })
}
