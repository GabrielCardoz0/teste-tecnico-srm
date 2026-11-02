import { Button } from "@/components/ui/button"
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

export function PendingReceivablesList({
  state,
  assignorName,
  onPreviousPage,
  onNextPage,
  renderAction,
}: PendingReceivablesListProps) {
  if (state.status === "loading") {
    return <p className="text-muted-foreground">Carregando recebíveis...</p>
  }

  if (state.status === "error") {
    return (
      <div role="alert" className="flex flex-col gap-1 text-destructive">
        <p className="font-medium">{state.problem.title}</p>
        {state.problem.detail && <p>{state.problem.detail}</p>}
      </div>
    )
  }

  const { page } = state
  if (page.totalElements === 0) {
    return <p className="text-muted-foreground">Nenhum recebível pendente.</p>
  }

  return (
    <div className="flex flex-col gap-3">
      <table className="w-full border-collapse text-left">
        <thead className="border-b text-muted-foreground">
          <tr>
            <th className="py-2 font-normal">Cedente</th>
            <th className="py-2 font-normal">Tipo</th>
            <th className="py-2 text-right font-normal">Valor de face</th>
            <th className="py-2 text-right font-normal">Vencimento</th>
            {renderAction && <th className="py-2" />}
          </tr>
        </thead>
        <tbody>
          {page.content.map((receivable) => (
            <tr key={receivable.id} className="border-b last:border-b-0">
              <td className="py-2">{assignorName(receivable.assignorId)}</td>
              <td className="py-2">
                {RECEIVABLE_TYPE_LABELS[receivable.type]}
              </td>
              <td className="py-2 text-right tabular-nums">
                {formatBrl(receivable.faceValue)}
              </td>
              <td className="py-2 text-right tabular-nums">
                {formatDate(receivable.dueDate)}
              </td>
              {renderAction && (
                <td className="py-2 text-right">{renderAction(receivable)}</td>
              )}
            </tr>
          ))}
        </tbody>
      </table>

      <div className="flex items-center justify-between">
        <span className="text-muted-foreground">
          Página {page.page + 1} de {page.totalPages} · {page.totalElements}{" "}
          {page.totalElements === 1 ? "recebível" : "recebíveis"}
        </span>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={onPreviousPage}
            disabled={page.page === 0}
          >
            Anterior
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={onNextPage}
            disabled={page.page + 1 >= page.totalPages}
          >
            Próxima
          </Button>
        </div>
      </div>
    </div>
  )
}
