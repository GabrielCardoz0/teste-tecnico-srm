import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import type { PaymentCurrency } from "@/lib/domain"

const OPTIONS: { value: PaymentCurrency; name: string; symbol: string }[] = [
  { value: "BRL", name: "Real", symbol: "R$" },
  { value: "USD", name: "Dólar", symbol: "US$" },
]

type CurrencyToggleProps = {
  value: PaymentCurrency
  onChange: (currency: PaymentCurrency) => void
  disabled?: boolean
  labelledBy?: string
}

export function CurrencyToggle({
  value,
  onChange,
  disabled,
  labelledBy,
}: CurrencyToggleProps) {
  return (
    <RadioGroup
      value={value}
      onValueChange={(next) => onChange(next as PaymentCurrency)}
      disabled={disabled}
      aria-labelledby={labelledBy}
      className="grid grid-cols-2 gap-2"
    >
      {OPTIONS.map((option) => (
        <label
          key={option.value}
          className="flex h-9 cursor-pointer items-center gap-2.5 rounded-lg border bg-background px-3 transition-colors hover:bg-muted/60 has-disabled:cursor-not-allowed has-disabled:opacity-50 has-data-checked:border-primary has-data-checked:bg-primary/5"
        >
          <RadioGroupItem value={option.value} />
          <span className="font-medium">{option.value}</span>
          <span className="text-muted-foreground">
            {option.name} ({option.symbol})
          </span>
        </label>
      ))}
    </RadioGroup>
  )
}
