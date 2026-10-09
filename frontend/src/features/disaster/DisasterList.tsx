import { EmptyState, Loading } from '../../shared/components'
import type { Disaster, DisasterPage } from './disaster-api'
import { severityLabels, typeLabels } from './DisasterForm'

export function DisasterList({ result, loading, selectedId, onSelect }: {
  result?: DisasterPage
  loading: boolean
  selectedId?: string
  onSelect: (disaster: Disaster) => void
}) {
  if (loading && !result) return <Loading>Đang tải danh sách thảm họa…</Loading>
  if (!result?.items.length) return <EmptyState>Không có thảm họa phù hợp bộ lọc.</EmptyState>
  return <div className="disaster-list" aria-busy={loading}>
    {result.items.map(disaster => <button type="button" key={disaster.id}
      className={`disaster-list__item${selectedId === disaster.id ? ' is-selected' : ''}`}
      aria-pressed={selectedId === disaster.id} onClick={() => onSelect(disaster)}>
      <span><strong>{disaster.name}</strong><small>{typeLabels[disaster.type]}</small></span>
      <span><span className={`severity severity--${disaster.severity.toLowerCase()}`}>{severityLabels[disaster.severity]}</span>
        <small>{disaster.status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã kết thúc'}</small></span>
    </button>)}
  </div>
}
