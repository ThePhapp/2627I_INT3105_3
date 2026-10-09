import { ErrorMessage, Loading } from '../../shared/components'
import type { Disaster } from './disaster-api'
import { severityLabels, typeLabels } from './DisasterForm'

const dateTime = new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' })

export function DisasterDetail({ disaster, loading, error, busy, confirming, onEdit, onConfirm, onResolve, onCancelResolve }: {
  disaster?: Disaster
  loading: boolean
  error: string
  busy: boolean
  confirming: boolean
  onEdit: () => void
  onConfirm: () => void
  onResolve: () => void
  onCancelResolve: () => void
}) {
  if (loading) return <Loading>Đang tải chi tiết…</Loading>
  if (error) return <ErrorMessage>{error}</ErrorMessage>
  if (!disaster) return <div className="disaster-panel__welcome"><p className="eyebrow">CHI TIẾT</p>
    <h2>Chọn một thảm họa</h2><p className="muted">Thông tin và thao tác sẽ hiển thị tại đây.</p></div>
  return <div className="disaster-detail">
    <div className="disaster-detail__heading"><div><p className="eyebrow">{typeLabels[disaster.type]}</p><h2>{disaster.name}</h2></div>
      <span className={`status status--${disaster.status.toLowerCase()}`}>{disaster.status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã kết thúc'}</span></div>
    <p className="disaster-detail__description">{disaster.description}</p>
    <dl className="disaster-facts">
      <div><dt>Mức độ</dt><dd>{severityLabels[disaster.severity]}</dd></div>
      <div><dt>Tọa độ</dt><dd>{disaster.latitude}, {disaster.longitude}</dd></div>
      <div><dt>Cập nhật</dt><dd>{dateTime.format(new Date(disaster.updatedAt))}</dd></div>
      <div><dt>Phiên bản</dt><dd>{disaster.version}</dd></div>
    </dl>
    {disaster.status === 'ACTIVE' && !confirming && <div className="disaster-actions">
      <button type="button" onClick={onEdit} disabled={busy}>Chỉnh sửa</button>
      <button type="button" className="danger-secondary" onClick={onConfirm} disabled={busy}>Kết thúc thảm họa</button>
    </div>}
    {confirming && <div className="resolve-confirmation" role="alertdialog" aria-labelledby="resolve-title" aria-describedby="resolve-description">
      <h3 id="resolve-title">Xác nhận kết thúc?</h3>
      <p id="resolve-description">Thảm họa đã kết thúc sẽ không thể mở lại hoặc chỉnh sửa.</p>
      <div className="disaster-actions"><button type="button" className="danger" onClick={onResolve} disabled={busy}>{busy ? 'Đang xử lý…' : 'Xác nhận kết thúc'}</button>
        <button type="button" className="secondary" onClick={onCancelResolve} disabled={busy}>Quay lại</button></div>
    </div>}
  </div>
}
