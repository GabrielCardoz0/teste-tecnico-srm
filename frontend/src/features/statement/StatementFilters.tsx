import { FilterXIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
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
import type { PaymentCurrency } from "@/lib/domain"

import type { StatementFilters as Filters } from "./useStatementFilters"

const ALL = "ALL"

const CURRENCY_ITEMS: Record<string, string> = {
  [ALL]: "Todas as moedas",
  BRL: "Real (BRL)",
  USD: "Dólar (USD)",
}

type StatementFiltersProps = {
  filters: Filters
  assignors: AssignorsState
  periodError?: string
  hasActiveFilters: boolean
  onChange: <F extends keyof Filters>(field: F, value: Filters[F]) => void
  onClear: () => void
}

export function StatementFilters({
  filters,
  assignors,
  periodError,
  hasActiveFilters,
  onChange,
  onClear,
}: StatementFiltersProps) {
  const assignorItems: Record<string, string> = {
    [ALL]: "Todos os cedentes",
    ...(assignors.status === "success"
      ? Object.fromEntries(
          assignors.assignors.map((assignor) => [
            String(assignor.id),
            assignor.legalName,
          ])
        )
      : {}),
  }

  return (
    <Card>
      <CardContent className="grid items-start gap-4 sm:grid-cols-2 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,2fr)_minmax(0,1.2fr)_auto]">
        <div className="flex flex-col gap-2">
          <Label htmlFor="statementFrom">De</Label>
          <Input
            id="statementFrom"
            type="date"
            value={filters.from}
            onChange={(event) => onChange("from", event.target.value)}
          />
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="statementTo">Até</Label>
          <Input
            id="statementTo"
            type="date"
            value={filters.to}
            onChange={(event) => onChange("to", event.target.value)}
            aria-invalid={!!periodError}
            aria-describedby={periodError ? "statementTo-error" : undefined}
          />
          {periodError && (
            <p id="statementTo-error" className="text-xs text-destructive">
              {periodError}
            </p>
          )}
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="statementAssignor">Cedente</Label>
          <Select
            id="statementAssignor"
            items={assignorItems}
            value={filters.assignorId || ALL}
            onValueChange={(value) =>
              onChange("assignorId", !value || value === ALL ? "" : value)
            }
          >
            <SelectTrigger className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {Object.entries(assignorItems).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="statementCurrency">Moeda</Label>
          <Select
            id="statementCurrency"
            items={CURRENCY_ITEMS}
            value={filters.paymentCurrency || ALL}
            onValueChange={(value) =>
              onChange(
                "paymentCurrency",
                !value || value === ALL ? "" : (value as PaymentCurrency)
              )
            }
          >
            <SelectTrigger className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {Object.entries(CURRENCY_ITEMS).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex flex-col gap-2">
          <span className="hidden text-sm leading-none select-none lg:block">
            &nbsp;
          </span>
          <Button
            variant="ghost"
            onClick={onClear}
            disabled={!hasActiveFilters}
          >
            <FilterXIcon />
            Limpar
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
