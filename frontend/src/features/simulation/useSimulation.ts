import { useEffect, useState } from "react"

import { problemOf, type ProblemDetail } from "@/lib/api"
import { useDebouncedValue } from "@/lib/useDebouncedValue"

import {
  simulate,
  type SimulationRequest,
  type SimulationResponse,
} from "./api"

const DEBOUNCE_MS = 400

export type SimulationState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; result: SimulationResponse }
  | { status: "error"; problem: ProblemDetail }

type Settled = {
  request: SimulationRequest
  outcome: Extract<SimulationState, { status: "success" | "error" }>
}

export function useSimulation(
  request: SimulationRequest | null
): SimulationState {
  const debouncedRequest = useDebouncedValue(request, DEBOUNCE_MS)
  const [settled, setSettled] = useState<Settled | null>(null)

  useEffect(() => {
    if (!debouncedRequest) {
      return
    }
    const controller = new AbortController()
    simulate(debouncedRequest, controller.signal)
      .then((result) =>
        setSettled({
          request: debouncedRequest,
          outcome: { status: "success", result },
        })
      )
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setSettled({
          request: debouncedRequest,
          outcome: { status: "error", problem: problemOf(error) },
        })
      })
    return () => controller.abort()
  }, [debouncedRequest])

  if (!request) {
    return { status: "idle" }
  }
  if (settled?.request === request) {
    return settled.outcome
  }
  return { status: "loading" }
}
