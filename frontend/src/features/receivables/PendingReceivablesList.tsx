import { InboxIcon } from "lucide-react"

import { ProblemAlert } from "@/components/ProblemAlert"
import { Badge } from "@/components/ui/badge"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardFooter } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { formatDate } from "@/lib/datetime"
import { RECEIVABLE_TYPE_LABELS } from "@/lib/domain"
import { formatBrl } from "@/lib/money"

import type { Receivable } from "./api"
import type { PendingReceivablesState } from "./usePendingReceivables"

type PendingReceivablesListProps = {
  state: PendingReceivablesState
  assignorName: (assignorId: number) => string
  onPreviousPage: () => void
  onNextPage: () => void
  renderAction?: (receivable: Receivable) => React.ReactNode
}

const HEADER_CELL =
  "px-4 py-3 text-xs font-medium tracking-wide text-muted-foreground uppercase"

export function PendingReceivablesList({
  state,
  assignorName,
  onPreviousPage,
  onNextPage,
  renderAction,
}: PendingReceivablesListProps) {
  if (state.status === "error") {
    return <ProblemAlert problem={state.problem} />
  }

  if (state.status === "success" && state.page.totalElements === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
          <div className="flex size-10 items-center justify-center rounded-full bg-muted">
            <InboxIcon className="size-5 text-muted-foreground" />
          </div>
          <div className="flex flex-col gap-1">
            <p className="font-medium">Nenhum recebível pendente</p>
            <p className="text-muted-foreground">
              Os recebíveis cadastrados no painel aparecem aqui para liquidação.
            </p>
          </div>
          <a
            href="#/painel"
            className={buttonVariants({ variant: "outline", size: "sm" })}
          >
            Ir para o painel
          </a>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="gap-0 py-0">
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-left">
          <thead className="border-b bg-muted/50">
            <tr>
              <th className={HEADER_CELL}>Cedente</th>
              <th className={HEADER_CELL}>Tipo</th>
              <th className={`${HEADER_CELL} text-right`}>Valor de face</th>
              <th className={`${HEADER_CELL} text-right`}>Vencimento</th>
              {renderAction && (
                <th className={HEADER_CELL}>
                  <span className="sr-only">Ações</span>
                </th>
              )}
            </tr>
          </thead>
          <tbody>
            {state.status === "loading"
              ? [0, 1, 2].map((row) => (
                  <tr key={row} className="border-b last:border-b-0">
                    <td className="px-4 py-3">
                      <Skeleton className="h-4 w-48" />
                    </td>
                    <td className="px-4 py-3">
                      <Skeleton className="h-5 w-28" />
                    </td>
                    <td className="px-4 py-3">
                      <Skeleton className="ml-auto h-4 w-24" />
                    </td>
                    <td className="px-4 py-3">
                      <Skeleton className="ml-auto h-4 w-20" />
                    </td>
                    {renderAction && <td className="px-4 py-3" />}
                  </tr>
                ))
              : state.page.content.map((receivable) => (
                  <tr
                    key={receivable.id}
                    className="border-b transition-colors last:border-b-0 hover:bg-muted/30"
                  >
                    <td className="px-4 py-3 font-medium">
                      {assignorName(receivable.assignorId)}
                    </td>
                    <td className="px-4 py-3">
                      <Badge variant="secondary">
                        {RECEIVABLE_TYPE_LABELS[receivable.type]}
                      </Badge>
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      {formatBrl(receivable.faceValue)}
                    </td>
                    <td className="px-4 py-3 text-right tabular-nums">
                      {formatDate(receivable.dueDate)}
                    </td>
                    {renderAction && (
                      <td className="px-4 py-3 text-right">
                        {renderAction(receivable)}
                      </td>
                    )}
                  </tr>
                ))}
          </tbody>
        </table>
      </div>

      {state.status === "success" && (
        <CardFooter className="justify-between">
          <span className="text-muted-foreground">
            Página {state.page.page + 1} de {state.page.totalPages} ·{" "}
            {state.page.totalElements}{" "}
            {state.page.totalElements === 1 ? "recebível" : "recebíveis"}
          </span>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={onPreviousPage}
              disabled={state.page.page === 0}
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={onNextPage}
              disabled={state.page.page + 1 >= state.page.totalPages}
            >
              Próxima
            </Button>
          </div>
        </CardFooter>
      )}
    </Card>
  )
}
