import { useMemo } from "react"

import { Button } from "@/components/ui/button"
import type { AssignorsState } from "@/features/assignors/useAssignors"
import { DuplicateDialog } from "@/features/receivables/DuplicateDialog"
import { RegistrationStatus } from "@/features/receivables/RegistrationStatus"
import { useReceivableRegistration } from "@/features/receivables/useReceivableRegistration"
import type { SimulationRequest } from "@/features/simulation/api"
import { SimulationResult } from "@/features/simulation/SimulationResult"
import { useSimulation } from "@/features/simulation/useSimulation"

import { OperatorForm } from "./OperatorForm"
import { useOperatorForm } from "./useOperatorForm"

export function OperatorPanel({ assignors }: { assignors: AssignorsState }) {
  const form = useOperatorForm()
  const registration = useReceivableRegistration({
    onRegistered: form.reset,
    onFieldErrors: form.applyServerErrors,
  })
  const { type, dueDate, paymentCurrency } = form.values
  const { faceValue } = form

  const simulationRequest = useMemo<SimulationRequest | null>(
    () =>
      type && faceValue && dueDate
        ? { type, faceValue, dueDate, paymentCurrency }
        : null,
    [type, faceValue, dueDate, paymentCurrency]
  )
  const simulation = useSimulation(simulationRequest)

  function handleRegister() {
    const request = form.validateRegistration()
    if (request) {
      void registration.register(request)
    }
  }

  return (
    <div className="grid gap-8 md:grid-cols-2">
      <section className="flex flex-col gap-4">
        <h2 className="font-medium">Recebível</h2>
        <OperatorForm
          values={form.values}
          errors={form.errors}
          assignors={assignors}
          onChange={form.setField}
        />
        <Button onClick={handleRegister} disabled={registration.busy}>
          {registration.busy ? "Cadastrando..." : "Cadastrar recebível"}
        </Button>
        <RegistrationStatus state={registration.state} />
      </section>

      <section className="flex flex-col gap-4" aria-live="polite">
        <h2 className="font-medium">Simulação</h2>
        <SimulationResult state={simulation} />
      </section>

      <DuplicateDialog
        open={registration.state.status === "confirming"}
        onConfirm={() => void registration.confirm()}
        onCancel={registration.cancel}
      />
    </div>
  )
}
