const BRAZILIAN_AMOUNT = /^(\d{1,3}(\.\d{3})+|\d+)(,\d{1,2})?$/

export function parseBrazilianAmount(input: string): string | null {
  const text = input.trim()
  if (!BRAZILIAN_AMOUNT.test(text)) {
    return null
  }
  const [integerPart, fraction = ""] = text.split(",")
  const digits = integerPart.replaceAll(".", "")
  if (!/[1-9]/.test(digits + fraction)) {
    return null
  }
  return fraction ? `${digits}.${fraction}` : digits
}

export function formatBrazilianAmount(normalized: string): string {
  const [integerPart, fraction = ""] = normalized.split(".")
  const digits = integerPart.replace(/^0+(?=\d)/, "")
  const grouped = digits.replace(/\B(?=(\d{3})+(?!\d))/g, ".")
  return `${grouped},${fraction.padEnd(2, "0")}`
}

const brl = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
})

const usd = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "USD",
})

const rate = new Intl.NumberFormat("pt-BR", {
  style: "percent",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

const exchangeRate = new Intl.NumberFormat("pt-BR", {
  minimumFractionDigits: 4,
  maximumFractionDigits: 8,
})

export function formatBrl(value: number): string {
  return brl.format(value)
}

export function formatUsd(value: number): string {
  return usd.format(value)
}

export function formatRate(fraction: number): string {
  return rate.format(fraction)
}

export function formatExchangeRate(value: number): string {
  return exchangeRate.format(value)
}
