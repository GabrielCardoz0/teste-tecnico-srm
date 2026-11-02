const dateTime = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short",
  timeZone: "America/Sao_Paulo",
})

export function formatDateTime(isoInstant: string): string {
  return dateTime.format(new Date(isoInstant))
}

export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split("-")
  return `${day}/${month}/${year}`
}

const dateOnly = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeZone: "America/Sao_Paulo",
})

const timeOnly = new Intl.DateTimeFormat("pt-BR", {
  timeStyle: "short",
  timeZone: "America/Sao_Paulo",
})

export function formatDateOf(isoInstant: string): string {
  return dateOnly.format(new Date(isoInstant))
}

export function formatTimeOf(isoInstant: string): string {
  return timeOnly.format(new Date(isoInstant))
}
