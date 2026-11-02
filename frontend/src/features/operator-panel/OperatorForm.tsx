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
import {
  PAYMENT_CURRENCY_LABELS,
  RECEIVABLE_TYPE_LABELS,
  type PaymentCurrency,
  type ReceivableType,
} from "@/lib/domain"

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
}

export function OperatorForm({
  values,
  errors,
  assignors,
  onChange,
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
    <div className="flex flex-col gap-4">
      <Field id="assignorId" label="Cedente" error={errors.assignorId}>
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

      <Field id="faceValue" label="Valor de face (R$)" error={errors.faceValue}>
        <Input
          id="faceValue"
          inputMode="decimal"
          placeholder="100.000,00"
          value={values.faceValue}
          onChange={(event) => onChange("faceValue", event.target.value)}
          aria-invalid={!!errors.faceValue}
          aria-describedby={errors.faceValue ? "faceValue-error" : undefined}
        />
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
        id="paymentCurrency"
        label="Moeda de pagamento"
        error={errors.paymentCurrency}
      >
        <Select
          id="paymentCurrency"
          items={PAYMENT_CURRENCY_LABELS}
          value={values.paymentCurrency}
          onValueChange={(value) =>
            value && onChange("paymentCurrency", value as PaymentCurrency)
          }
        >
          <SelectTrigger className="w-full">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {Object.entries(PAYMENT_CURRENCY_LABELS).map(([value, label]) => (
              <SelectItem key={value} value={value}>
                {label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </Field>
    </div>
  )
}

function Field({
  id,
  label,
  error,
  children,
}: {
  id: string
  label: string
  error?: string
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={id}>{label}</Label>
      {children}
      {error && (
        <p id={`${id}-error`} className="text-xs text-destructive">
          {error}
        </p>
      )}
    </div>
  )
}
