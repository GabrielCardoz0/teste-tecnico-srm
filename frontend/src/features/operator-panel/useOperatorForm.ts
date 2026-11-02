import { useState } from "react"

import type { FieldViolation } from "@/lib/api"
import type { PaymentCurrency, ReceivableType } from "@/lib/domain"
import { formatBrazilianAmount, parseBrazilianAmount } from "@/lib/money"

export type OperatorFormValues = {
  assignorId: string
  type: ReceivableType | ""
  faceValue: string
  dueDate: string
  paymentCurrency: PaymentCurrency
}

export type OperatorFormField = keyof OperatorFormValues

export type OperatorFormErrors = Partial<Record<OperatorFormField, string>>

export type ValidRegistration = {
  assignorId: number
  type: ReceivableType
  faceValue: string
  dueDate: string
}

const EMPTY_FORM: OperatorFormValues = {
  assignorId: "",
  type: "",
  faceValue: "",
  dueDate: "",
  paymentCurrency: "BRL",
}

const REQUIRED = "Campo obrigatório."

export function useOperatorForm() {
  const [values, setValues] = useState<OperatorFormValues>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<OperatorFormErrors>({})

  function setField<F extends OperatorFormField>(
    field: F,
    value: OperatorFormValues[F]
  ) {
    setValues((current) => ({ ...current, [field]: value }))
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
  }

  function formatFaceValue() {
    const normalized = parseBrazilianAmount(values.faceValue)
    if (normalized) {
      setValues((current) => ({
        ...current,
        faceValue: formatBrazilianAmount(normalized),
      }))
    }
  }

  function applyServerErrors(violations: FieldViolation[]) {
    setFieldErrors(
      Object.fromEntries(
        violations
          .filter((violation) => violation.field in EMPTY_FORM)
          .map((violation) => [violation.field, violation.message])
      )
    )
  }

  function validateRegistration(): ValidRegistration | null {
    const missing: OperatorFormErrors = {}
    if (!values.assignorId) missing.assignorId = REQUIRED
    if (!values.type) missing.type = REQUIRED
    if (!values.faceValue.trim()) missing.faceValue = REQUIRED
    if (!values.dueDate) missing.dueDate = REQUIRED
    setFieldErrors(missing)

    if (!values.assignorId || !values.type || !faceValue || !values.dueDate) {
      return null
    }
    return {
      assignorId: Number(values.assignorId),
      type: values.type,
      faceValue,
      dueDate: values.dueDate,
    }
  }

  function reset() {
    setValues(EMPTY_FORM)
    setFieldErrors({})
  }

  const faceValue = parseBrazilianAmount(values.faceValue)
  const errors: OperatorFormErrors = { ...fieldErrors }
  if (values.faceValue.trim() !== "" && faceValue === null) {
    errors.faceValue =
      "Valor inválido. Use o formato 100.000,50 (até 2 casas decimais)."
  }

  return {
    values,
    setField,
    formatFaceValue,
    faceValue,
    errors,
    applyServerErrors,
    validateRegistration,
    reset,
  }
}
