import { CurrencyToggle } from "@/components/CurrencyToggle"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import type { AssignorsState } from "@/features/assignors/useAssignors"
import { RECEIVABLE_TYPE_LABELS, type ReceivableType } from "@/lib/domain"
import { cn } from "@/lib/utils"

import type {
  OperatorFormErrors,
  OperatorFormField,
  OperatorFormValues,
} from "./useOperatorForm"

type OperatorFormProps = {
  values: OperatorFormValues
  errors: OperatorFormErrors
  assignors: AssignorsState
  onChange: <F extends OperatorFormField>(
    field: F,
    value: OperatorFormValues[F]
  ) => void
  onFaceValueBlur: () => void
}

export function OperatorForm({
  values,
  errors,
  assignors,
  onChange,
  onFaceValueBlur,
}: OperatorFormProps) {
  const assignorItems =
    assignors.status === "success"
      ? Object.fromEntries(
          assignors.assignors.map((assignor) => [
            String(assignor.id),
            assignor.legalName,
          ])
        )
      : {}

  return (
    <div className="grid gap-x-4 gap-y-5 sm:grid-cols-2">
      <Field
        id="assignorId"
        label="Cedente"
        error={errors.assignorId}
        className="sm:col-span-2"
      >
        <Select
          id="assignorId"
          items={assignorItems}
          value={values.assignorId || null}
          onValueChange={(value) => onChange("assignorId", value ?? "")}
          disabled={assignors.status !== "success"}
        >
          <SelectTrigger className="w-full" aria-invalid={!!errors.assignorId}>
            <SelectValue
              placeholder={
                assignors.status === "error"
                  ? "Não foi possível carregar os cedentes"
                  : assignors.status === "loading"
                    ? "Carregando cedentes..."
                    : "Selecione o cedente"
              }
            />
          </SelectTrigger>
          <SelectContent>
            {Object.entries(assignorItems).map(([id, legalName]) => (
              <SelectItem key={id} value={id}>
                {legalName}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </Field>

      <Field id="type" label="Tipo de recebível" error={errors.type}>
        <Select
          id="type"
          items={RECEIVABLE_TYPE_LABELS}
          value={values.type || null}
          onValueChange={(value) =>
            onChange("type", (value as ReceivableType | null) ?? "")
          }
        >
          <SelectTrigger className="w-full" aria-invalid={!!errors.type}>
            <SelectValue placeholder="Selecione o tipo" />
          </SelectTrigger>
          <SelectContent>
            {Object.entries(RECEIVABLE_TYPE_LABELS).map(([value, label]) => (
              <SelectItem key={value} value={value}>
                {label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </Field>

      <Field id="dueDate" label="Vencimento" error={errors.dueDate}>
        <Input
          id="dueDate"
          type="date"
          value={values.dueDate}
          onChange={(event) => onChange("dueDate", event.target.value)}
          aria-invalid={!!errors.dueDate}
          aria-describedby={errors.dueDate ? "dueDate-error" : undefined}
        />
      </Field>

      <Field
        id="faceValue"
        label="Valor de face"
        error={errors.faceValue}
        hint="Use vírgula para os centavos. Ex.: 100.000,50"
        className="sm:col-span-2"
      >
        <div className="relative">
          <span className="pointer-events-none absolute inset-y-0 left-2.5 flex items-center text-muted-foreground">
            R$
          </span>
          <Input
            id="faceValue"
            inputMode="decimal"
            autoComplete="off"
            placeholder="0,00"
            className="pl-9 tabular-nums"
            value={values.faceValue}
            onChange={(event) => onChange("faceValue", event.target.value)}
            onBlur={onFaceValueBlur}
            aria-invalid={!!errors.faceValue}
            aria-describedby={
              errors.faceValue ? "faceValue-error" : "faceValue-hint"
            }
          />
        </div>
      </Field>

      <Field
        id="paymentCurrency"
        label="Moeda de pagamento"
        hint="Usada apenas na simulação; a moeda final é escolhida na liquidação."
        className="sm:col-span-2"
      >
        <CurrencyToggle
          value={values.paymentCurrency}
          onChange={(currency) => onChange("paymentCurrency", currency)}
          labelledBy="paymentCurrency-label"
        />
      </Field>
    </div>
  )
}

function Field({
  id,
  label,
  error,
  hint,
  className,
  children,
}: {
  id: string
  label: string
  error?: string
  hint?: string
  className?: string
  children: React.ReactNode
}) {
  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Label id={`${id}-label`} htmlFor={id}>
        {label}
      </Label>
      {children}
      {error ? (
        <p id={`${id}-error`} className="text-xs text-destructive">
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-hint`} className="text-xs text-muted-foreground">
            {hint}
          </p>
        )
      )}
    </div>
  )
}
