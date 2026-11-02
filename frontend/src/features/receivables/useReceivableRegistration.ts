import { useRef, useState } from "react"

import { problemOf, type FieldViolation, type ProblemDetail } from "@/lib/api"

import {
  createReceivable,
  hasSimilarReceivable,
  type CreateReceivableRequest,
  type Receivable,
} from "./api"

export type RegistrationState =
  | { status: "idle" }
  | { status: "checking" }
  | { status: "confirming"; request: CreateReceivableRequest }
  | { status: "submitting" }
  | { status: "success"; receivable: Receivable }
  | { status: "error"; problem: ProblemDetail }

type Callbacks = {
  onRegistered: (receivable: Receivable) => void
  onFieldErrors: (violations: FieldViolation[]) => void
}

export function useReceivableRegistration({
  onRegistered,
  onFieldErrors,
}: Callbacks) {
  const [state, setState] = useState<RegistrationState>({ status: "idle" })
  const inFlight = useRef(false)

  async function run(task: () => Promise<void>) {
    if (inFlight.current) {
      return
    }
    inFlight.current = true
    try {
      await task()
    } catch (error) {
      const problem = problemOf(error)
      if (problem.status === 400 && problem.errors?.length) {
        onFieldErrors(problem.errors)
        setState({ status: "idle" })
      } else {
        setState({ status: "error", problem })
      }
    } finally {
      inFlight.current = false
    }
  }

  async function submit(request: CreateReceivableRequest) {
    setState({ status: "submitting" })
    const receivable = await createReceivable(request)
    setState({ status: "success", receivable })
    onRegistered(receivable)
  }

  function register(request: CreateReceivableRequest) {
    return run(async () => {
      setState({ status: "checking" })
      if (await hasSimilarReceivable(request)) {
        setState({ status: "confirming", request })
        return
      }
      await submit(request)
    })
  }

  function confirm() {
    if (state.status === "confirming") {
      const { request } = state
      return run(() => submit(request))
    }
  }

  function cancel() {
    setState({ status: "idle" })
  }

  const busy = state.status === "checking" || state.status === "submitting"

  return { state, busy, register, confirm, cancel }
}
