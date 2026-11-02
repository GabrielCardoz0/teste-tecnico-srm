import { useSyncExternalStore } from "react"

function subscribe(onChange: () => void) {
  window.addEventListener("hashchange", onChange)
  return () => window.removeEventListener("hashchange", onChange)
}

function getHash() {
  return window.location.hash
}

export function useHashRoute<R extends string>(
  routes: readonly R[],
  fallback: R
): R {
  const path = useSyncExternalStore(subscribe, getHash).replace(/^#\/?/, "")
  return (routes as readonly string[]).includes(path) ? (path as R) : fallback
}
