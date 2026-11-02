import type { AssignorsState } from "@/features/assignors/useAssignors"

import { PendingReceivablesList } from "./PendingReceivablesList"
import { usePendingReceivables } from "./usePendingReceivables"

export function PendingReceivablesPage({
  assignors,
}: {
  assignors: AssignorsState
}) {
  const pending = usePendingReceivables()
  const names = new Map(
    assignors.status === "success"
      ? assignors.assignors.map((assignor) => [assignor.id, assignor.legalName])
      : []
  )

  return (
    <section className="flex flex-col gap-4">
      <h2 className="font-medium">Recebíveis pendentes</h2>
      <PendingReceivablesList
        state={pending.state}
        assignorName={(id) => names.get(id) ?? `Cedente #${id}`}
        onPreviousPage={pending.previousPage}
        onNextPage={pending.nextPage}
      />
    </section>
  )
}
