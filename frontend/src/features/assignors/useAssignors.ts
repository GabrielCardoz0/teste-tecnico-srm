import { useEffect, useState } from "react"

import { problemOf, type ProblemDetail } from "@/lib/api"

import { listAssignors, type Assignor } from "./api"

export type AssignorsState =
  | { status: "loading" }
  | { status: "success"; assignors: Assignor[] }
  | { status: "error"; problem: ProblemDetail }

export function useAssignors(): AssignorsState {
  const [state, setState] = useState<AssignorsState>({ status: "loading" })

  useEffect(() => {
    const controller = new AbortController()
    listAssignors(controller.signal)
      .then((assignors) => setState({ status: "success", assignors }))
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return
        }
        setState({ status: "error", problem: problemOf(error) })
      })
    return () => controller.abort()
  }, [])

  return state
}
