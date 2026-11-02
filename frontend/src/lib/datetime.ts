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
