import type { ReactNode } from 'react'
import type { Role } from '../auth/session'

export type FeatureRoute = { path: string; label: string; roles: readonly Role[]; element: ReactNode }
export type FeatureRouteModule = { routes: FeatureRoute[] }
// A feature exports `routes` from src/features/<feature>/routes.tsx only when implemented.
const modules = import.meta.glob<FeatureRouteModule>('../features/**/routes.tsx', { eager: true })
export const featureRoutes = Object.values(modules).flatMap(module => module.routes)
const paths = new Set<string>()
for (const route of featureRoutes) {
  if (!route.path.startsWith('/') || route.path === '/login' || paths.has(route.path)) throw new Error('Invalid or duplicate feature route')
  paths.add(route.path)
}
export function landingPath(role: Role) {
  const preferred = role === 'CITIZEN' ? '/my-reports' : '/operations/reports'
  return featureRoutes.some(route => route.path === preferred && route.roles.includes(role)) ? preferred : '/login'
}
