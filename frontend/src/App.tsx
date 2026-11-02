import { AssignorList } from "@/features/assignors/AssignorList"
import { useAssignors } from "@/features/assignors/useAssignors"

export function App() {
  const assignors = useAssignors()

  return (
    <main className="mx-auto flex min-h-svh max-w-2xl flex-col gap-4 p-6 text-sm">
      <h1 className="text-lg font-medium">SRM Credit Engine</h1>
      <h2 className="font-medium">Cedentes ativos</h2>
      <AssignorList state={assignors} />
    </main>
  )
}

export default App
