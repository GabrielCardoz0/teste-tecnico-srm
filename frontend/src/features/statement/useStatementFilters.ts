import { useMemo, useState } from "react"

import type { PaymentCurrency } from "@/lib/domain"

import type { StatementQuery } from "./api"

export type StatementFilters = {
  from: string
  to: string
  assignorId: string
  paymentCurrency: PaymentCurrency | ""
}

export const PAGE_SIZES = [10, 20, 50] as const

export type PageSize = (typeof PAGE_SIZES)[number]

const EMPTY_FILTERS: StatementFilters = {
  from: "",
  to: "",
  assignorId: "",
  paymentCurrency: "",
}

export function useStatementFilters() {
  const [filters, setFilters] = useState<StatementFilters>(EMPTY_FILTERS)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState<PageSize>(20)

  function setFilter<F extends keyof StatementFilters>(
    field: F,
    value: StatementFilters[F]
  ) {
    setFilters((current) => ({ ...current, [field]: value }))
    setPage(0)
  }

  function changeSize(next: PageSize) {
    setSize(next)
    setPage(0)
  }

  function clear() {
    setFilters(EMPTY_FILTERS)
    setPage(0)
  }

  const periodError =
    filters.from && filters.to && filters.from > filters.to
      ? "A data final deve ser igual ou posterior à data inicial."
      : undefined

  const query = useMemo<StatementQuery | null>(
    () =>
      periodError
        ? null
        : {
            from: filters.from || undefined,
            to: filters.to || undefined,
            assignorId: filters.assignorId
              ? Number(filters.assignorId)
              : undefined,
            paymentCurrency: filters.paymentCurrency || undefined,
            page,
            size,
          },
    [filters, page, size, periodError]
  )

  const hasActiveFilters = Object.values(filters).some((value) => value !== "")

  return {
    filters,
    setFilter,
    clear,
    hasActiveFilters,
    periodError,
    page,
    previousPage: () => setPage((current) => Math.max(0, current - 1)),
    nextPage: () => setPage((current) => current + 1),
    size,
    changeSize,
    query,
  }
}
