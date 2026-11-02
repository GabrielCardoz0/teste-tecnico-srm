import { SearchXIcon, TriangleAlertIcon } from "lucide-react"

import { ProblemAlert } from "@/components/ProblemAlert"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter } from "@/components/ui/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { formatCnpj } from "@/lib/cnpj"
import { formatDateOf, formatTimeOf } from "@/lib/datetime"
import { RECEIVABLE_TYPE_LABELS } from "@/lib/domain"
import { formatBrl, formatExchangeRate, formatUsd } from "@/lib/money"
import { cn } from "@/lib/utils"

import type { StatementLine } from "./api"
import type { StatementView } from "./useSettlementStatement"
import { PAGE_SIZES, type PageSize } from "./useStatementFilters"

const PAGE_SIZE_ITEMS = Object.fromEntries(
  PAGE_SIZES.map((size) => [String(size), `${size} por página`])
)

const COLUMNS: { label: string; numeric?: boolean }[] = [
  { label: "Liquidado em" },
  { label: "Cedente" },
  { label: "Tipo" },
  { label: "Valor de face", numeric: true },
  { label: "Valor presente", numeric: true },
  { label: "Deságio", numeric: true },
  { label: "Moeda" },
  { label: "Cotação", numeric: true },
  { label: "Valor final", numeric: true },
]

type StatementTableProps = {
  view: StatementView
  size: PageSize
  periodError?: string
  hasActiveFilters: boolean
  onSizeChange: (size: PageSize) => void
  onPreviousPage: () => void
  onNextPage: () => void
  onClearFilters: () => void
}

export function StatementTable({
  view,
  size,
  periodError,
  hasActiveFilters,
  onSizeChange,
  onPreviousPage,
  onNextPage,
  onClearFilters,
}: StatementTableProps) {
  const { page, loading, problem } = view

  if (!page && problem) {
    return <ProblemAlert problem={problem} />
  }

  const periodAlert = periodError && (
    <Alert>
      <TriangleAlertIcon />
      <AlertTitle>Período inválido</AlertTitle>
      <AlertDescription>
        Corrija as datas para atualizar. A tabela mostra a última consulta
        válida.
      </AlertDescription>
    </Alert>
  )

  if (page && page.totalElements === 0 && !loading) {
    return (
      <div className="flex flex-col gap-3">
        {periodAlert}
        <Card>
          <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
            <div className="flex size-10 items-center justify-center rounded-full bg-muted">
              <SearchXIcon className="size-5 text-muted-foreground" />
            </div>
            <div className="flex flex-col gap-1">
              <p className="font-medium">Nenhuma liquidação encontrada</p>
              <p className="text-muted-foreground">
                {hasActiveFilters
                  ? "Nenhum resultado para esses filtros. Ajuste o período, o cedente ou a moeda."
                  : "As liquidações registradas aparecem aqui."}
              </p>
            </div>
            {hasActiveFilters && (
              <Button variant="outline" size="sm" onClick={onClearFilters}>
                Limpar filtros
              </Button>
            )}
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      {periodAlert}
      {page && problem && <ProblemAlert problem={problem} />}

      <Card className="gap-0 py-0">
        <Table
          aria-busy={loading}
          className={cn("transition-opacity", loading && page && "opacity-50")}
        >
          <TableHeader className="bg-muted/50">
            <TableRow>
              {COLUMNS.map((column) => (
                <TableHead
                  key={column.label}
                  className={cn(
                    "h-10 px-3 text-xs font-medium tracking-wide text-muted-foreground uppercase",
                    column.numeric && "text-right"
                  )}
                >
                  {column.label}
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {page
              ? page.content.map((line) => (
                  <StatementRow key={line.settlementId} line={line} />
                ))
              : Array.from({ length: 5 }, (_, row) => (
                  <TableRow key={row}>
                    {COLUMNS.map((column) => (
                      <TableCell key={column.label} className="px-3 py-3">
                        <Skeleton
                          className={cn(
                            "h-4 w-20",
                            column.numeric && "ml-auto"
                          )}
                        />
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
          </TableBody>
        </Table>

        <CardFooter className="flex-wrap justify-between gap-3">
          <span className="text-muted-foreground">
            {page
              ? `${page.totalElements} ${page.totalElements === 1 ? "liquidação" : "liquidações"} · Página ${page.page + 1} de ${Math.max(page.totalPages, 1)}`
              : "Carregando..."}
          </span>
          <div className="flex items-center gap-2">
            <Select
              items={PAGE_SIZE_ITEMS}
              value={String(size)}
              onValueChange={(value) =>
                value && onSizeChange(Number(value) as PageSize)
              }
            >
              <SelectTrigger size="sm" aria-label="Itens por página">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Object.entries(PAGE_SIZE_ITEMS).map(([value, label]) => (
                  <SelectItem key={value} value={value}>
                    {label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Button
              variant="outline"
              size="sm"
              onClick={onPreviousPage}
              disabled={loading || !page || page.page === 0}
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={onNextPage}
              disabled={loading || !page || page.page + 1 >= page.totalPages}
            >
              Próxima
            </Button>
          </div>
        </CardFooter>
      </Card>
    </div>
  )
}

function StatementRow({ line }: { line: StatementLine }) {
  const usd = line.paymentCurrency === "USD"

  return (
    <TableRow>
      <TableCell className="px-3 py-3 whitespace-nowrap tabular-nums">
        <span className="block">{formatDateOf(line.settledAt)}</span>
        <span className="block text-xs text-muted-foreground">
          {formatTimeOf(line.settledAt)}
        </span>
      </TableCell>
      <TableCell className="px-3 py-3">
        <span className="block font-medium">{line.assignorLegalName}</span>
        <span className="block text-xs text-muted-foreground tabular-nums">
          {formatCnpj(line.assignorCnpj)}
        </span>
      </TableCell>
      <TableCell className="px-3 py-3">
        <Badge variant="secondary">
          {RECEIVABLE_TYPE_LABELS[line.receivableType]}
        </Badge>
      </TableCell>
      <TableCell className="px-3 py-3 text-right tabular-nums">
        {formatBrl(line.faceValue)}
      </TableCell>
      <TableCell className="px-3 py-3 text-right tabular-nums">
        {formatBrl(line.presentValueBrl)}
      </TableCell>
      <TableCell className="px-3 py-3 text-right tabular-nums">
        {formatBrl(line.discountBrl)}
      </TableCell>
      <TableCell className="px-3 py-3">
        <Badge variant={usd ? "default" : "outline"}>
          {line.paymentCurrency}
        </Badge>
      </TableCell>
      <TableCell className="px-3 py-3 text-right text-muted-foreground tabular-nums">
        {usd && line.exchangeRate !== undefined
          ? formatExchangeRate(line.exchangeRate)
          : "—"}
      </TableCell>
      <TableCell className="px-3 py-3 text-right font-medium tabular-nums">
        {usd ? formatUsd(line.paymentAmount) : formatBrl(line.paymentAmount)}
      </TableCell>
    </TableRow>
  )
}
