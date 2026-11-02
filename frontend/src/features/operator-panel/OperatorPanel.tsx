import { useMemo } from "react"

import { PageHeader } from "@/components/PageHeader"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
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
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Painel do operador"
        description="Informe o recebível para simular o valor líquido e cadastrá-lo."
      />

      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_380px]">
        <Card>
          <CardHeader>
            <CardTitle>Dados do recebível</CardTitle>
            <CardDescription>
              A simulação é atualizada automaticamente enquanto você digita.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-5">
            <OperatorForm
              values={form.values}
              errors={form.errors}
              assignors={assignors}
              onChange={form.setField}
              onFaceValueBlur={form.formatFaceValue}
            />
            <RegistrationStatus state={registration.state} />
          </CardContent>
          <CardFooter className="justify-end">
            <Button onClick={handleRegister} disabled={registration.busy}>
              {registration.busy ? "Cadastrando..." : "Cadastrar recebível"}
            </Button>
          </CardFooter>
        </Card>

        <Card className="lg:sticky lg:top-6">
          <CardHeader>
            <CardTitle>Simulação</CardTitle>
            <CardDescription>
              Calculada pelo motor de precificação.
            </CardDescription>
          </CardHeader>
          <CardContent aria-live="polite">
            <SimulationResult
              state={simulation}
              idleHint={
                form.errors.faceValue
                  ? "Corrija o valor de face para ver a simulação."
                  : undefined
              }
            />
          </CardContent>
        </Card>
      </div>

      <DuplicateDialog
        open={registration.state.status === "confirming"}
        onConfirm={() => void registration.confirm()}
        onCancel={registration.cancel}
      />
    </div>
  )
}
