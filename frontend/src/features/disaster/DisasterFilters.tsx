import {
  disasterStatuses,
  disasterTypes,
  type DisasterFilters as Filters,
  type DisasterStatus,
  type DisasterType,
} from './disaster-api'
import { statusLabels, typeLabels } from './DisasterBadges'

export function DisasterFilters({ filters, onChange }: {
  filters: Filters
  onChange: (next: Partial<Filters>) => void
}) {
  return <fieldset className="disaster-filters">
    <legend className="sr-only">Bộ lọc và sắp xếp thảm họa</legend>
    <div><label htmlFor="filter-status">Trạng thái</label><span className="disaster-select">
      <select id="filter-status" value={filters.status ?? ''}
        onChange={event => onChange({ status: event.target.value as DisasterStatus || undefined })}>
        <option value="">Tất cả trạng thái</option>
        {disasterStatuses.map(status => <option key={status} value={status}>{statusLabels[status]}</option>)}
      </select>
    </span></div>
    <div><label htmlFor="filter-type">Loại</label><span className="disaster-select">
      <select id="filter-type" value={filters.type ?? ''}
        onChange={event => onChange({ type: event.target.value as DisasterType || undefined })}>
        <option value="">Tất cả loại</option>
        {disasterTypes.map(type => <option key={type} value={type}>{typeLabels[type]}</option>)}
      </select>
    </span></div>
    <div><label htmlFor="filter-sort">Sắp xếp</label><span className="disaster-select">
      <select id="filter-sort" value={filters.sort}
        onChange={event => onChange({ sort: event.target.value as Filters['sort'] })}>
        <option value="createdAt,desc">Mới nhất trước</option>
        <option value="createdAt,asc">Cũ nhất trước</option>
      </select>
    </span></div>
  </fieldset>
}
