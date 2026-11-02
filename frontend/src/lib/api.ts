export type FieldViolation = {
  field: string
  message: string
}

export type ProblemDetail = {
  type?: string
  title: string
  status: number
  detail?: string
  instance?: string
  errors?: FieldViolation[]
}

export class ApiError extends Error {
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title)
    this.name = "ApiError"
    this.problem = problem
  }
}

const API_PREFIX = "/api"

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {}
): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set("Accept", "application/json")
  if (init.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json")
  }

  let response: Response
  try {
    response = await fetch(API_PREFIX + path, { ...init, headers })
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw error
    }
    throw new ApiError({
      title: "Falha de conexão",
      status: 0,
      detail: "Não foi possível conectar ao servidor.",
    })
  }

  if (!response.ok) {
    throw new ApiError(await toProblemDetail(response))
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function toProblemDetail(response: Response): Promise<ProblemDetail> {
  const contentType = response.headers.get("Content-Type") ?? ""
  if (contentType.includes("json")) {
    const body: unknown = await response.json().catch(() => null)
    if (isProblemDetail(body)) {
      return { ...body, status: body.status ?? response.status }
    }
  }
  return {
    title: response.statusText || "Erro inesperado",
    status: response.status,
  }
}

function isProblemDetail(
  body: unknown
): body is Omit<ProblemDetail, "status"> & { status?: number } {
  return (
    typeof body === "object" &&
    body !== null &&
    typeof (body as { title?: unknown }).title === "string"
  )
}
