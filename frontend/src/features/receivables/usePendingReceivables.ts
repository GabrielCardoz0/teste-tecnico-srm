import { useEffect, useState } from "react"

import { problemOf, type Page, type ProblemDetail } from "@/lib/api"

import { listReceivables, type Receivable } from "./api"

const PAGE_SIZE = 10

export type PendingReceivablesState =
  | { status: "loading" }
  | { status: "success"; page: Page<Receivable> }
  | { status: "error"; problem: ProblemDetail }

type Settled = {
  key: string
  outcome: Exclude<PendingReceivablesState, { status: "loading" }>
}

export function usePendingReceivables() {
  const [page, setPage] = useState(0)
  const [reloadCount, setReloadCount] = useState(0)
  const [settled, setSettled] = useState<Settled | null>(null)
  const key = `${page}:${reloadCount}`

  useEffect(() => {
    const controller = new AbortController()
    listReceivables(
      { status: "PENDING", page, size: PAGE_SIZE },
      controller.signal
    )
      .then((result) => {
        if (result.content.length === 0 && page > 0) {
          setPage(page - 1)
          return
        }
        setSettled({ key, outcome: { status: "success", page: result } })
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setSettled({
          key,
          outcome: { status: "error", problem: problemOf(error) },
        })
      })
    return () => controller.abort()
  }, [page, key])

  const state: PendingReceivablesState =
    settled?.key === key ? settled.outcome : { status: "loading" }

  return {
    state,
    previousPage: () => setPage((current) => Math.max(0, current - 1)),
    nextPage: () => setPage((current) => current + 1),
    reload: () => setReloadCount((current) => current + 1),
  }
}
