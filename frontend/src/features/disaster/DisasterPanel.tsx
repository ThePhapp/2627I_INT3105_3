import { EmptyState, ErrorMessage, Loading } from '../../shared/components'
import type { Disaster } from './disaster-api'
import { ResolveDisasterDialog } from './ResolveDisasterDialog'
import { SeverityBadge, StatusBadge, typeLabels } from './DisasterBadges'

const dateTime = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' })

export function DisasterDetail({ disaster, loading, error, busy, confirming, onEdit, onConfirm, onResolve, onCancelResolve, onRetry }: {
  disaster?: Disaster
  loading: boolean
  error: string
  busy: boolean
  confirming: boolean
  onEdit: () => void
  onConfirm: () => void
  onResolve: () => void
  onCancelResolve: () => void
  onRetry: () => void
}) {
  if (loading) return <Loading>Đang tải chi tiết…</Loading>
  if (error) return <div className="disaster-panel__error"><ErrorMessage>{error}</ErrorMessage>
    <button type="button" className="secondary" onClick={onRetry}>Tải lại chi tiết</button></div>
  if (!disaster) return <div className="disaster-panel__welcome"><EmptyState>
    <strong>Chọn một thảm họa</strong><span>Thông tin và thao tác sẽ hiển thị tại đây.</span>
  </EmptyState></div>
  return <div className="disaster-detail">
    <div className="disaster-detail__heading"><div><p className="eyebrow">{typeLabels[disaster.type]}</p><h2>{disaster.name}</h2></div>
      <StatusBadge status={disaster.status} /></div>
    <section className="disaster-detail__section" aria-labelledby="disaster-description-title">
      <h3 id="disaster-description-title">Mô tả tình hình</h3>
      <p className="disaster-detail__description">{disaster.description}</p>
    </section>
    <dl className="disaster-facts">
      <div><dt>Mức độ</dt><dd><SeverityBadge severity={disaster.severity} /></dd></div>
      <div><dt>Tọa độ</dt><dd>{disaster.latitude}, {disaster.longitude}</dd></div>
      <div><dt>Tạo lúc</dt><dd><time dateTime={disaster.createdAt}>{dateTime.format(new Date(disaster.createdAt))}</time></dd></div>
      <div><dt>Cập nhật</dt><dd>{dateTime.format(new Date(disaster.updatedAt))}</dd></div>
      <div><dt>Mã thảm họa</dt><dd className="disaster-facts__id">{disaster.id}</dd></div>
      <div><dt>Phiên bản</dt><dd>{disaster.version}</dd></div>
    </dl>
    {disaster.status === 'ACTIVE' && !confirming && <div className="disaster-actions">
      <button type="button" onClick={onEdit} disabled={busy}>Chỉnh sửa</button>
      <button type="button" className="danger-secondary" onClick={onConfirm} disabled={busy}>Kết thúc thảm họa</button>
    </div>}
    {confirming && <ResolveDisasterDialog disasterName={disaster.name} busy={busy}
      onResolve={onResolve} onCancel={onCancelResolve} />}
  </div>
}
