import { useRef, useState } from "react"

import type { Receivable } from "@/features/receivables/api"
import { problemOf, type ProblemDetail } from "@/lib/api"
import type { PaymentCurrency } from "@/lib/domain"

import { settle, type Settlement } from "./api"

export type SettlementStatus =
  | { status: "editing" }
  | { status: "submitting" }
  | { status: "success"; settlement: Settlement }
  | { status: "conflict"; problem: ProblemDetail }
  | { status: "rejected"; problem: ProblemDetail }
  | { status: "failed"; problem: ProblemDetail }

type Attempt = {
  idempotencyKey: string
  sentCurrency: PaymentCurrency | null
}

function newAttempt(): Attempt {
  return { idempotencyKey: crypto.randomUUID(), sentCurrency: null }
}

function isRetryable(problem: ProblemDetail) {
  return problem.status === 0 || problem.status >= 500
}

export function useSettlement() {
  const [receivable, setReceivable] = useState<Receivable | null>(null)
  const [currency, setCurrency] = useState<PaymentCurrency>("BRL")
  const [state, setState] = useState<SettlementStatus>({ status: "editing" })
  const attempt = useRef<Attempt | null>(null)
  const inFlight = useRef(false)

  function start(target: Receivable) {
    attempt.current = newAttempt()
    setReceivable(target)
    setCurrency("BRL")
    setState({ status: "editing" })
  }

  function close() {
    attempt.current = null
    setReceivable(null)
  }

  function chooseCurrency(next: PaymentCurrency) {
    setCurrency(next)
    setState({ status: "editing" })
  }

  async function confirm() {
    if (!receivable || !attempt.current || inFlight.current) {
      return
    }
    const sent = attempt.current.sentCurrency
    if (sent !== null && sent !== currency) {
      attempt.current = newAttempt()
    }
    attempt.current.sentCurrency = currency

    inFlight.current = true
    setState({ status: "submitting" })
    try {
      const settlement = await settle(
        { receivableId: receivable.id, paymentCurrency: currency },
        attempt.current.idempotencyKey
      )
      attempt.current = null
      setState({ status: "success", settlement })
    } catch (error) {
      const problem = problemOf(error)
      if (problem.status === 409) {
        setState({ status: "conflict", problem })
      } else if (isRetryable(problem)) {
        setState({ status: "failed", problem })
      } else {
        setState({ status: "rejected", problem })
      }
    } finally {
      inFlight.current = false
    }
  }

  return {
    receivable,
    currency,
    state,
    start,
    close,
    chooseCurrency,
    confirm,
  }
}
