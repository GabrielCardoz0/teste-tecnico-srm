import { Button } from "@/components/ui/button"
import type { AssignorsState } from "@/features/assignors/useAssignors"
import { SettlementDialog } from "@/features/settlements/SettlementDialog"
import { useSettlement } from "@/features/settlements/useSettlement"

import { PendingReceivablesList } from "./PendingReceivablesList"
import { usePendingReceivables } from "./usePendingReceivables"

export function PendingReceivablesPage({
  assignors,
}: {
  assignors: AssignorsState
}) {
  const pending = usePendingReceivables()
  const settlement = useSettlement()
  const names = new Map(
    assignors.status === "success"
      ? assignors.assignors.map((assignor) => [assignor.id, assignor.legalName])
      : []
  )
  const assignorName = (id: number) => names.get(id) ?? `Cedente #${id}`

  function closeSettlement() {
    const changed =
      settlement.state.status === "success" ||
      settlement.state.status === "conflict"
    settlement.close()
    if (changed) {
      pending.reload()
    }
  }

  return (
    <section className="flex flex-col gap-4">
      <h2 className="font-medium">Recebíveis pendentes</h2>
      <PendingReceivablesList
        state={pending.state}
        assignorName={assignorName}
        onPreviousPage={pending.previousPage}
        onNextPage={pending.nextPage}
        renderAction={(receivable) => (
          <Button size="sm" onClick={() => settlement.start(receivable)}>
            Liquidar
          </Button>
        )}
      />
      <SettlementDialog
        receivable={settlement.receivable}
        assignorName={assignorName}
        currency={settlement.currency}
        state={settlement.state}
        onCurrencyChange={settlement.chooseCurrency}
        onConfirm={() => void settlement.confirm()}
        onClose={closeSettlement}
      />
    </section>
  )
}
