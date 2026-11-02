import { useState } from "react"

import type { PaymentCurrency, ReceivableType } from "@/lib/domain"
import { parseBrazilianAmount } from "@/lib/money"

export type OperatorFormValues = {
  assignorId: string
  type: ReceivableType | ""
  faceValue: string
  dueDate: string
  paymentCurrency: PaymentCurrency
}

export type OperatorFormField = keyof OperatorFormValues

export type OperatorFormErrors = Partial<Record<OperatorFormField, string>>

const EMPTY_FORM: OperatorFormValues = {
  assignorId: "",
  type: "",
  faceValue: "",
  dueDate: "",
  paymentCurrency: "BRL",
}

export function useOperatorForm() {
  const [values, setValues] = useState<OperatorFormValues>(EMPTY_FORM)

  function setField<F extends OperatorFormField>(
    field: F,
    value: OperatorFormValues[F]
  ) {
    setValues((current) => ({ ...current, [field]: value }))
  }

  const faceValue = parseBrazilianAmount(values.faceValue)
  const errors: OperatorFormErrors = {}
  if (values.faceValue.trim() !== "" && faceValue === null) {
    errors.faceValue =
      "Valor inválido. Use o formato 100.000,50 (até 2 casas decimais)."
  }

  return { values, setField, faceValue, errors }
}
