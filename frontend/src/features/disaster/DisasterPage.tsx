import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../../shared/api'
import { ErrorMessage } from '../../shared/components'
import { DisasterForm } from './DisasterForm'
import { DisasterList } from './DisasterList'
import { DisasterDetail } from './DisasterPanel'
import {
  createDisaster, disasterStatuses, disasterTypes, getDisaster, listDisasters, updateDisaster,
  type Disaster, type DisasterFields, type DisasterFilters, type DisasterPage as Page, type DisasterPatch,
  type DisasterStatus, type DisasterType,
} from './disaster-api'
import './disaster.css'

const pageSize = 20
type Mode = 'detail' | 'create' | 'edit'

function message(error: unknown) {
  if (error instanceof ApiError) return error.message
  if (error instanceof DOMException && error.name === 'AbortError') return ''
  return 'Không thể kết nối máy chủ. Vui lòng thử lại.'
}

export function DisasterPage() {
  const [filters, setFilters] = useState<DisasterFilters>({ page: 0, size: pageSize, sort: 'createdAt,desc' })
  const [result, setResult] = useState<Page>()
  const [listLoading, setListLoading] = useState(true)
  const [listError, setListError] = useState('')
  const [selected, setSelected] = useState<Disaster>()
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')
  const [mode, setMode] = useState<Mode>('detail')
  const [busy, setBusy] = useState(false)
  const [mutationError, setMutationError] = useState('')
  const [confirming, setConfirming] = useState(false)

  const loadList = useCallback(async () => {
    setListLoading(true); setListError('')
    try { setResult(await listDisasters(filters)) }
    catch (error) { setListError(message(error)) }
    finally { setListLoading(false) }
  }, [filters])

  useEffect(() => { void loadList() }, [loadList])

  async function select(disaster: Disaster) {
    setSelected(disaster); setMode('detail'); setConfirming(false); setMutationError(''); setDetailError(''); setDetailLoading(true)
    try { setSelected(await getDisaster(disaster.id)) }
    catch (error) { setDetailError(message(error)) }
    finally { setDetailLoading(false) }
  }

  function changeFilter(next: Partial<DisasterFilters>) {
    setFilters(current => ({ ...current, ...next, page: next.page ?? 0 }))
    setSelected(undefined); setMode('detail'); setConfirming(false); setMutationError('')
  }

  async function afterMutation(disaster: Disaster) {
    setSelected(disaster); setMode('detail'); setConfirming(false)
    await loadList()
  }

  async function mutate(action: () => Promise<Disaster>) {
    if (busy) return
    setBusy(true); setMutationError('')
    try { await afterMutation(await action()) }
    catch (error) {
      setMutationError(message(error))
      if (error instanceof ApiError && error.status === 409 && selected) {
        try { setSelected(await getDisaster(selected.id)); await loadList(); setMode('detail'); setConfirming(false) }
        catch { /* Keep the conflict visible if refresh also fails. */ }
      }
    } finally { setBusy(false) }
  }

  const startCreate = () => { setSelected(undefined); setMode('create'); setConfirming(false); setMutationError('') }
  const create = (fields: DisasterFields) => mutate(() => createDisaster(fields))
  const edit = (patch: DisasterPatch) => selected ? mutate(() => updateDisaster(selected.id, patch)) : Promise.resolve()
  const resolve = () => selected ? mutate(() => updateDisaster(selected.id, { expectedVersion: selected.version, status: 'RESOLVED' })) : Promise.resolve()

  return <section className="disaster-page">
    <div className="disaster-page__header"><div><p className="eyebrow">ĐIỀU PHỐI / THẢM HỌA</p><h1>Quản lý thảm họa</h1>
      <p className="muted">Theo dõi sự kiện, cập nhật thông tin và kết thúc khi tình hình đã được xử lý.</p></div>
      <button type="button" onClick={startCreate}>Tạo thảm họa</button></div>

    <div className="disaster-filters" aria-label="Bộ lọc thảm họa">
      <div><label htmlFor="filter-status">Trạng thái</label><select id="filter-status" value={filters.status ?? ''}
        onChange={event => changeFilter({ status: event.target.value as DisasterStatus || undefined })}>
        <option value="">Tất cả</option>{disasterStatuses.map(status => <option key={status} value={status}>{status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã kết thúc'}</option>)}
      </select></div>
      <div><label htmlFor="filter-type">Loại</label><select id="filter-type" value={filters.type ?? ''}
        onChange={event => changeFilter({ type: event.target.value as DisasterType || undefined })}>
        <option value="">Tất cả</option>{disasterTypes.map(type => <option key={type} value={type}>{type}</option>)}</select></div>
      <div><label htmlFor="filter-sort">Sắp xếp</label><select id="filter-sort" value={filters.sort}
        onChange={event => changeFilter({ sort: event.target.value as DisasterFilters['sort'] })}>
        <option value="createdAt,desc">Mới nhất</option><option value="createdAt,asc">Cũ nhất</option></select></div>
    </div>

    {listError && <div className="disaster-list-error"><ErrorMessage>{listError}</ErrorMessage><button type="button" className="secondary" onClick={() => void loadList()}>Thử lại</button></div>}
    <div className="disaster-workspace">
      <div className="disaster-master">
        <div className="disaster-master__summary"><strong>{result?.totalElements ?? 0} thảm họa</strong>{listLoading && result && <span>Đang cập nhật…</span>}</div>
        <DisasterList result={result} loading={listLoading} selectedId={selected?.id} onSelect={disaster => void select(disaster)} />
        {result && result.totalPages > 1 && <div className="pagination" aria-label="Phân trang">
          <button type="button" className="secondary" disabled={filters.page === 0 || listLoading} onClick={() => changeFilter({ page: filters.page - 1 })}>Trang trước</button>
          <span>Trang {result.page + 1} / {result.totalPages}</span>
          <button type="button" className="secondary" disabled={result.page + 1 >= result.totalPages || listLoading} onClick={() => changeFilter({ page: filters.page + 1 })}>Trang sau</button>
        </div>}
      </div>
      <aside className="disaster-panel" aria-label={mode === 'create' ? 'Tạo thảm họa' : mode === 'edit' ? 'Chỉnh sửa thảm họa' : 'Chi tiết thảm họa'}>
        {mutationError && <ErrorMessage>{mutationError}</ErrorMessage>}
        {mode === 'create' && <><p className="eyebrow">THẢM HỌA MỚI</p><h2>Tạo thảm họa</h2>
          <DisasterForm busy={busy} onCancel={() => setMode('detail')} onCreate={create} onEdit={edit} /></>}
        {mode === 'edit' && selected && <><p className="eyebrow">CẬP NHẬT</p><h2>Chỉnh sửa thảm họa</h2>
          <DisasterForm key={`${selected.id}-${selected.version}`} disaster={selected} busy={busy} onCancel={() => setMode('detail')} onCreate={create} onEdit={edit} /></>}
        {mode === 'detail' && <DisasterDetail disaster={selected} loading={detailLoading} error={detailError} busy={busy}
          confirming={confirming} onEdit={() => { setMode('edit'); setMutationError('') }} onConfirm={() => setConfirming(true)}
          onResolve={() => void resolve()} onCancelResolve={() => setConfirming(false)} />}
      </aside>
    </div>
  </section>
}
