import { useAssignors } from "@/features/assignors/useAssignors"
import { OperatorPanel } from "@/features/operator-panel/OperatorPanel"
import { useHashRoute } from "@/lib/useHashRoute"

const ROUTES = ["painel"] as const

type Route = (typeof ROUTES)[number]

const NAVIGATION: { route: Route; label: string }[] = [
  { route: "painel", label: "Painel do operador" },
]

export function App() {
  const route = useHashRoute(ROUTES, "painel")
  const assignors = useAssignors()

  return (
    <main className="mx-auto flex min-h-svh max-w-4xl flex-col gap-6 p-6 text-sm">
      <header className="flex flex-col gap-3">
        <h1 className="text-lg font-medium">SRM Credit Engine</h1>
        <nav aria-label="Principal">
          <ul className="flex gap-4 border-b">
            {NAVIGATION.map((item) => (
              <li key={item.route}>
                <a
                  href={`#/${item.route}`}
                  aria-current={route === item.route ? "page" : undefined}
                  className="-mb-px block border-b-2 border-transparent pb-2 text-muted-foreground hover:text-foreground aria-[current=page]:border-foreground aria-[current=page]:text-foreground"
                >
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>
      </header>

      {route === "painel" && <OperatorPanel assignors={assignors} />}
    </main>
  )
}

export default App
