const BRAZILIAN_AMOUNT = /^(\d{1,3}(\.\d{3})+|\d+)(,\d{1,2})?$/

const MAX_INTEGER_DIGITS = 17

function groupThousands(digits: string): string {
  return digits.replace(/\B(?=(\d{3})+(?!\d))/g, ".")
}

export function maskCentsInput(raw: string, pasted = false): string {
  let digits = raw.replace(/\D/g, "")
  if (pasted) {
    const decimals = raw.trim().match(/[.,](\d{1,2})$/)
    if (!decimals) {
      digits += "00"
    } else if (decimals[1].length === 1) {
      digits += "0"
    }
  }
  digits = digits.replace(/^0+/, "").slice(0, MAX_INTEGER_DIGITS + 2)
  if (!digits) {
    return ""
  }
  const padded = digits.padStart(3, "0")
  return `${groupThousands(padded.slice(0, -2))},${padded.slice(-2)}`
}

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
