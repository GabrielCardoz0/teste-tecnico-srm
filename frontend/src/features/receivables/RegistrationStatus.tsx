import type { RegistrationState } from "./useReceivableRegistration"

export function RegistrationStatus({ state }: { state: RegistrationState }) {
  if (state.status === "success") {
    return (
      <p role="status" className="text-emerald-700 dark:text-emerald-400">
        Recebível #{state.receivable.id} cadastrado como pendente.
      </p>
    )
  }

  if (state.status === "error") {
    const { problem } = state
    return (
      <div role="alert" className="flex flex-col gap-1 text-destructive">
        <p className="font-medium">
          {problem.status === 422
            ? "Não foi possível cadastrar"
            : problem.title}
        </p>
        {problem.detail && <p>{problem.detail}</p>}
      </div>
    )
  }

  return null
}
