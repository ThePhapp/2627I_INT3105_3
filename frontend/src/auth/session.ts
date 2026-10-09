import { useSyncExternalStore } from 'react'

export type Role = 'CITIZEN' | 'AUTHORITY'
export type User = { id: string; email: string; role: Role }
type Session = { user: User | null; expiresAt: string | null; generation: number }
let state: Session = { user: null, expiresAt: null, generation: 0 }
let token: string | null = null
let expiration: ReturnType<typeof setTimeout> | undefined
const listeners = new Set<() => void>()
const cleanups = new Set<() => void>()

export const session = {
  snapshot: () => state,
  token: () => token,
  subscribe(listener: () => void) { listeners.add(listener); return () => { listeners.delete(listener) } },
  // Feature owners register cleanup for any per-user cache outside mounted components.
  onClear(cleanup: () => void) { cleanups.add(cleanup); return () => { cleanups.delete(cleanup) } },
  clear() {
    clearTimeout(expiration)
    token = null
    state = { user: null, expiresAt: null, generation: state.generation + 1 }
    for (const cleanup of cleanups) cleanup()
    for (const listener of listeners) listener()
  },
  establish(accessToken: string, user: User, expiresAt: string, generation: number) {
    if (generation !== state.generation) return false
    const remaining = Date.parse(expiresAt) - Date.now()
    if (!Number.isFinite(remaining) || remaining <= 0) return false
    token = accessToken
    state = { ...state, user, expiresAt }
    expiration = setTimeout(() => session.clear(), remaining)
    for (const listener of listeners) listener()
    return true
  },
}

export function useSession() {
  return useSyncExternalStore(session.subscribe, session.snapshot)
}
