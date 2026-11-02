export type ReceivableType = "DUPLICATA_MERCANTIL" | "CHEQUE_PRE_DATADO"

export type PaymentCurrency = "BRL" | "USD"

export const RECEIVABLE_TYPE_LABELS: Record<ReceivableType, string> = {
  DUPLICATA_MERCANTIL: "Duplicata Mercantil",
  CHEQUE_PRE_DATADO: "Cheque Pré-datado",
}

export const PAYMENT_CURRENCY_LABELS: Record<PaymentCurrency, string> = {
  BRL: "Real (BRL)",
  USD: "Dólar (USD)",
}
