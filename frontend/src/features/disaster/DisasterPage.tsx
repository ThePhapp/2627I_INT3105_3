import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError } from '../../shared/api'
import { ErrorMessage, PageHeader } from '../../shared/components'
import { DisasterFilters } from './DisasterFilters'
import { DisasterForm } from './DisasterForm'
import { DisasterList } from './DisasterList'
import { DisasterPagination } from './DisasterPagination'
import { DisasterDetail } from './DisasterPanel'
import {
  createDisaster, getDisaster, listDisasters, updateDisaster,
  type Disaster, type DisasterFields, type DisasterFilters as Filters, type DisasterPage as Page, type DisasterPatch,
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
  const [filters, setFilters] = useState<Filters>({ page: 0, size: pageSize, sort: 'createdAt,desc' })
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
  const panel = useRef<HTMLElement>(null)

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

  function changeFilter(next: Partial<Filters>) {
    setFilters(current => ({ ...current, ...next, page: next.page ?? 0 }))
    setSelected(undefined); setMode('detail'); setConfirming(false); setMutationError('')
  }

  async function afterMutation(disaster: Disaster) {
    setSelected(disaster); setMode('detail'); setConfirming(false)
    await loadList()
    requestAnimationFrame(() => panel.current?.focus())
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
    <PageHeader eyebrow="ĐIỀU PHỐI / THẢM HỌA" title="Quản lý thảm họa"
      description="Theo dõi sự kiện, cập nhật thông tin và kết thúc khi tình hình đã được xử lý."
      action={<button type="button" onClick={startCreate}>Tạo thảm họa</button>} />

    <DisasterFilters filters={filters} onChange={changeFilter} />

    {listError && <div className="disaster-list-error"><ErrorMessage>{listError}</ErrorMessage><button type="button" className="secondary" onClick={() => void loadList()}>Thử lại</button></div>}
    <div className="disaster-workspace">
      <div className="disaster-master">
        <div className="disaster-master__summary"><strong>{result?.totalElements ?? 0} thảm họa</strong>
          {listLoading && result && <span role="status">Đang cập nhật…</span>}</div>
        <DisasterList result={result} loading={listLoading} selectedId={selected?.id} onSelect={disaster => void select(disaster)} />
        {result && <DisasterPagination result={result} disabled={listLoading}
          onPageChange={page => changeFilter({ page })} />}
      </div>
      <aside ref={panel} tabIndex={-1} className="disaster-panel"
        aria-label={mode === 'create' ? 'Tạo thảm họa' : mode === 'edit' ? 'Chỉnh sửa thảm họa' : 'Chi tiết thảm họa'}>
        {mutationError && <ErrorMessage>{mutationError}</ErrorMessage>}
        {mode === 'create' && <><p className="eyebrow">THẢM HỌA MỚI</p><h2>Tạo thảm họa</h2>
          <DisasterForm busy={busy} onCancel={() => setMode('detail')} onCreate={create} onEdit={edit} /></>}
        {mode === 'edit' && selected && <><p className="eyebrow">CẬP NHẬT</p><h2>Chỉnh sửa thảm họa</h2>
          <DisasterForm key={`${selected.id}-${selected.version}`} disaster={selected} busy={busy} onCancel={() => setMode('detail')} onCreate={create} onEdit={edit} /></>}
        {mode === 'detail' && <DisasterDetail disaster={selected} loading={detailLoading} error={detailError} busy={busy}
          confirming={confirming} onEdit={() => { setMode('edit'); setMutationError('') }} onConfirm={() => setConfirming(true)}
          onResolve={() => void resolve()} onCancelResolve={() => setConfirming(false)}
          onRetry={() => { if (selected) void select(selected) }} />}
      </aside>
    </div>
  </section>
}
