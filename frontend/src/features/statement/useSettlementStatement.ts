import { useEffect, useState } from "react"

import { problemOf, type Page, type ProblemDetail } from "@/lib/api"

import { listSettlements, type StatementLine, type StatementQuery } from "./api"

type Settled = {
  key: string
  page: Page<StatementLine> | null
  problem: ProblemDetail | null
}

export type StatementView = {
  page: Page<StatementLine> | null
  loading: boolean
  problem: ProblemDetail | null
}

export function useSettlementStatement(
  query: StatementQuery | null
): StatementView {
  const key = query ? JSON.stringify(query) : null
  const [settled, setSettled] = useState<Settled | null>(null)
  const [lastPage, setLastPage] = useState<Page<StatementLine> | null>(null)

  useEffect(() => {
    if (!key) {
      return
    }
    const controller = new AbortController()
    listSettlements(JSON.parse(key) as StatementQuery, controller.signal)
      .then((page) => {
        setSettled({ key, page, problem: null })
        setLastPage(page)
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setSettled({ key, page: null, problem: problemOf(error) })
      })
    return () => controller.abort()
  }, [key])

  const current = key !== null && settled?.key === key ? settled : null
  return {
    page: current?.page ?? lastPage,
    loading: key !== null && current === null,
    problem: current?.problem ?? null,
  }
}
