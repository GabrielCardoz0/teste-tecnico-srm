import { CircleAlertIcon } from "lucide-react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import type { ProblemDetail } from "@/lib/api"

export function ProblemAlert({
  title,
  problem,
}: {
  title?: string
  problem: ProblemDetail
}) {
  return (
    <Alert variant="destructive">
      <CircleAlertIcon />
      <AlertTitle>{title ?? problem.title}</AlertTitle>
      {problem.detail && <AlertDescription>{problem.detail}</AlertDescription>}
    </Alert>
  )
}
