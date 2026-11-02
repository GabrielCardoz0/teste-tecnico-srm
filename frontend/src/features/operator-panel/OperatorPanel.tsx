import { useMemo } from "react"

import { useAssignors } from "@/features/assignors/useAssignors"
import type { SimulationRequest } from "@/features/simulation/api"
import { SimulationResult } from "@/features/simulation/SimulationResult"
import { useSimulation } from "@/features/simulation/useSimulation"

import { OperatorForm } from "./OperatorForm"
import { useOperatorForm } from "./useOperatorForm"

export function OperatorPanel() {
  const assignors = useAssignors()
  const form = useOperatorForm()
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
      </section>

      <section className="flex flex-col gap-4" aria-live="polite">
        <h2 className="font-medium">Simulação</h2>
        <SimulationResult state={simulation} />
      </section>
    </div>
  )
}
