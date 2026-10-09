import { api } from '../../shared/api'

export const disasterTypes = ['EARTHQUAKE', 'FLOOD', 'TYPHOON', 'WILDFIRE', 'TSUNAMI'] as const
export const severities = ['LOW', 'MODERATE', 'HIGH', 'CRITICAL'] as const
export const disasterStatuses = ['ACTIVE', 'RESOLVED'] as const

export type DisasterType = typeof disasterTypes[number]
export type Severity = typeof severities[number]
export type DisasterStatus = typeof disasterStatuses[number]

export type Disaster = {
  id: string
  name: string
  type: DisasterType
  severity: Severity
  description: string
  latitude: number
  longitude: number
  status: DisasterStatus
  version: number
  createdAt: string
  updatedAt: string
}

export type DisasterPage = {
  items: Disaster[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type DisasterFilters = {
  page: number
  size: number
  sort: 'createdAt,desc' | 'createdAt,asc'
  status?: DisasterStatus
  type?: DisasterType
}

export type DisasterFields = Pick<Disaster, 'name' | 'type' | 'severity' | 'description' | 'latitude' | 'longitude'>
export type DisasterPatch = Partial<DisasterFields> & { expectedVersion: number; status?: DisasterStatus }

export function listDisasters(filters: DisasterFilters) {
  const query = new URLSearchParams({
    page: String(filters.page),
    size: String(filters.size),
    sort: filters.sort,
  })
  if (filters.status) query.set('status', filters.status)
  if (filters.type) query.set('type', filters.type)
  return api<DisasterPage>(`/api/disasters?${query}`)
}

export function getDisaster(id: string) {
  return api<Disaster>(`/api/disasters/${id}`)
}

export function createDisaster(fields: DisasterFields) {
  return api<Disaster>('/api/disasters', { method: 'POST', body: JSON.stringify(fields) })
}

export function updateDisaster(id: string, patch: DisasterPatch) {
  return api<Disaster>(`/api/disasters/${id}`, { method: 'PATCH', body: JSON.stringify(patch) })
}
