import { PageHeader } from "@/components/PageHeader"
import type { AssignorsState } from "@/features/assignors/useAssignors"

import { StatementFilters } from "./StatementFilters"
import { StatementTable } from "./StatementTable"
import { useSettlementStatement } from "./useSettlementStatement"
import { useStatementFilters } from "./useStatementFilters"

export function StatementPage({ assignors }: { assignors: AssignorsState }) {
  const filters = useStatementFilters()
  const view = useSettlementStatement(filters.query)

  return (
    <section className="flex flex-col gap-6">
      <PageHeader
        title="Extrato de liquidações"
        description="Histórico das liquidações registradas, com data e hora de Brasília."
      />
      <StatementFilters
        filters={filters.filters}
        assignors={assignors}
        periodError={filters.periodError}
        hasActiveFilters={filters.hasActiveFilters}
        onChange={filters.setFilter}
        onClear={filters.clear}
      />
      <StatementTable
        view={view}
        size={filters.size}
        periodError={filters.periodError}
        hasActiveFilters={filters.hasActiveFilters}
        onSizeChange={filters.changeSize}
        onPreviousPage={filters.previousPage}
        onNextPage={filters.nextPage}
        onClearFilters={filters.clear}
      />
    </section>
  )
}
