import { OperatorPanel } from "@/features/operator-panel/OperatorPanel"

export function App() {
  return (
    <main className="mx-auto flex min-h-svh max-w-4xl flex-col gap-6 p-6 text-sm">
      <h1 className="text-lg font-medium">SRM Credit Engine</h1>
      <OperatorPanel />
    </main>
  )
}

export default App
