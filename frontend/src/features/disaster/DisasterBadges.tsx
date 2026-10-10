import type { DisasterStatus, DisasterType, Severity } from './disaster-api'

export const typeLabels: Record<DisasterType, string> = {
  EARTHQUAKE: 'Động đất',
  FLOOD: 'Lũ lụt',
  TYPHOON: 'Bão',
  WILDFIRE: 'Cháy rừng',
  TSUNAMI: 'Sóng thần',
}

export const severityLabels: Record<Severity, string> = {
  LOW: 'Thấp',
  MODERATE: 'Trung bình',
  HIGH: 'Cao',
  CRITICAL: 'Nghiêm trọng',
}

export const statusLabels: Record<DisasterStatus, string> = {
  ACTIVE: 'Đang hoạt động',
  RESOLVED: 'Đã kết thúc',
}

export function SeverityBadge({ severity }: { severity: Severity }) {
  return <span className={`disaster-badge severity severity--${severity.toLowerCase()}`}>{severityLabels[severity]}</span>
}

export function StatusBadge({ status }: { status: DisasterStatus }) {
  return <span className={`disaster-badge status status--${status.toLowerCase()}`}>{statusLabels[status]}</span>
}
