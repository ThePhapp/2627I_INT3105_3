import { EmptyState, Loading } from '../../shared/components'
import type { Disaster, DisasterPage } from './disaster-api'
import { SeverityBadge, StatusBadge, typeLabels } from './DisasterBadges'

const date = new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })

export function DisasterList({ result, loading, selectedId, onSelect }: {
  result?: DisasterPage
  loading: boolean
  selectedId?: string
  onSelect: (disaster: Disaster) => void
}) {
  if (loading && !result) return <Loading>Đang tải danh sách thảm họa…</Loading>
  if (!result?.items.length) return <EmptyState>Không có thảm họa phù hợp bộ lọc.</EmptyState>
  return <div className="disaster-list" aria-busy={loading} aria-label="Danh sách thảm họa">
    {result.items.map(disaster => <button type="button" key={disaster.id}
      className={`disaster-list__item${selectedId === disaster.id ? ' is-selected' : ''}`}
      aria-pressed={selectedId === disaster.id} onClick={() => onSelect(disaster)}>
      <span className="disaster-list__identity"><strong title={disaster.name}>{disaster.name}</strong>
        <small>{typeLabels[disaster.type]} · <time dateTime={disaster.createdAt}>{date.format(new Date(disaster.createdAt))}</time></small></span>
      <span className="disaster-list__badges"><SeverityBadge severity={disaster.severity} /><StatusBadge status={disaster.status} /></span>
    </button>)}
  </div>
}
