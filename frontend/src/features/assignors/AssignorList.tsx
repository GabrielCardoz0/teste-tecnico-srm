import type { AssignorsState } from "./useAssignors"

export function AssignorList({ state }: { state: AssignorsState }) {
  if (state.status === "loading") {
    return <p className="text-muted-foreground">Carregando cedentes...</p>
  }

  if (state.status === "error") {
    return (
      <div role="alert" className="text-destructive">
        <p className="font-medium">{state.problem.title}</p>
        {state.problem.detail && <p>{state.problem.detail}</p>}
      </div>
    )
  }

  if (state.assignors.length === 0) {
    return <p className="text-muted-foreground">Nenhum cedente ativo.</p>
  }

  return (
    <ul className="divide-y rounded-md border">
      {state.assignors.map((assignor) => (
        <li key={assignor.id} className="flex justify-between gap-4 px-4 py-2">
          <span>{assignor.legalName}</span>
          <span className="font-mono text-muted-foreground">
            {assignor.cnpj}
          </span>
        </li>
      ))}
    </ul>
  )
}
