import { CircleCheckIcon } from "lucide-react"

import { ProblemAlert } from "@/components/ProblemAlert"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"

import type { RegistrationState } from "./useReceivableRegistration"

export function RegistrationStatus({ state }: { state: RegistrationState }) {
  if (state.status === "success") {
    return (
      <Alert
        role="status"
        className="border-emerald-600/30 bg-emerald-50 text-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-200"
      >
        <CircleCheckIcon />
        <AlertTitle>Recebível #{state.receivable.id} cadastrado</AlertTitle>
        <AlertDescription className="text-emerald-800 dark:text-emerald-300">
          Ele já aparece em Liquidações pendentes.
        </AlertDescription>
      </Alert>
    )
  }

  if (state.status === "error") {
    return (
      <ProblemAlert
        title={
          state.problem.status === 422
            ? "Não foi possível cadastrar"
            : undefined
        }
        problem={state.problem}
      />
    )
  }

  return null
}
