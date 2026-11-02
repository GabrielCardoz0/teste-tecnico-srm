import {
  CalculatorIcon,
  LandmarkIcon,
  ListChecksIcon,
  ReceiptTextIcon,
  type LucideIcon,
} from "lucide-react"

import { useAssignors } from "@/features/assignors/useAssignors"
import { OperatorPanel } from "@/features/operator-panel/OperatorPanel"
import { PendingReceivablesPage } from "@/features/receivables/PendingReceivablesPage"
import { StatementPage } from "@/features/statement/StatementPage"
import { useHashRoute } from "@/lib/useHashRoute"

const ROUTES = ["painel", "pendentes", "extrato"] as const

type Route = (typeof ROUTES)[number]

const NAVIGATION: { route: Route; label: string; icon: LucideIcon }[] = [
  { route: "painel", label: "Painel do operador", icon: CalculatorIcon },
  { route: "pendentes", label: "Liquidações pendentes", icon: ListChecksIcon },
  { route: "extrato", label: "Extrato", icon: ReceiptTextIcon },
]

export function App() {
  const route = useHashRoute(ROUTES, "painel")
  const assignors = useAssignors()

  return (
    <div className="min-h-svh bg-muted/40 text-sm">
      <header className="border-b bg-background">
        <div className="mx-auto max-w-7xl px-6">
          <div className="flex items-center gap-3 py-4">
            <div className="flex size-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <LandmarkIcon className="size-5" />
            </div>
            <div>
              <p className="font-heading text-base leading-tight font-semibold">
                SRM Credit Engine
              </p>
              <p className="text-xs text-muted-foreground">
                Cessão de crédito multimoedas
              </p>
            </div>
          </div>
          <nav aria-label="Principal">
            <ul className="-mb-px flex gap-6">
              {NAVIGATION.map((item) => (
                <li key={item.route}>
                  <a
                    href={`#/${item.route}`}
                    aria-current={route === item.route ? "page" : undefined}
                    className="flex items-center gap-2 border-b-2 border-transparent pb-3 text-muted-foreground transition-colors hover:text-foreground aria-[current=page]:border-primary aria-[current=page]:font-medium aria-[current=page]:text-foreground"
                  >
                    <item.icon className="size-4" />
                    {item.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-6 py-8">
        {route === "painel" && <OperatorPanel assignors={assignors} />}
        {route === "pendentes" && (
          <PendingReceivablesPage assignors={assignors} />
        )}
        {route === "extrato" && <StatementPage assignors={assignors} />}
      </main>
    </div>
  )
}

export default App
