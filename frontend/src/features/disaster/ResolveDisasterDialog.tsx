import { useEffect, useRef, type KeyboardEvent } from 'react'

export function ResolveDisasterDialog({ disasterName, busy, returnFocus, onResolve, onCancel }: {
  disasterName: string
  busy: boolean
  returnFocus?: HTMLElement | null
  onResolve: () => void
  onCancel: () => void
}) {
  const dialog = useRef<HTMLDivElement>(null)
  const cancel = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    cancel.current?.focus()
    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [])

  function close() {
    if (busy) return
    onCancel()
    if (returnFocus?.isConnected) returnFocus.focus()
  }

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape' && !busy) {
      event.preventDefault()
      close()
      return
    }
    if (event.key !== 'Tab') return
    const controls = dialog.current?.querySelectorAll<HTMLButtonElement>('button:not([disabled])')
    if (!controls?.length) return
    const first = controls[0]
    const last = controls[controls.length - 1]
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
  }

  return <div className="resolve-dialog-layer">
    <div ref={dialog} className="resolve-dialog" role="alertdialog" aria-modal="true"
      aria-labelledby="resolve-title" aria-describedby="resolve-description" onKeyDown={handleKeyDown}>
      <p className="eyebrow">THAO TÁC KHÔNG THỂ HOÀN TÁC</p>
      <h2 id="resolve-title">Kết thúc thảm họa?</h2>
      <p id="resolve-description">“{disasterName}” sẽ chuyển sang Đã kết thúc và không thể mở lại hoặc chỉnh sửa.</p>
      <div className="resolve-dialog__actions">
        <button ref={cancel} type="button" className="secondary" onClick={close}
          aria-disabled={busy}>Quay lại</button>
        <button type="button" className="danger" onClick={() => { if (!busy) onResolve() }}
          aria-busy={busy} aria-disabled={busy}>
          {busy ? 'Đang kết thúc…' : 'Xác nhận kết thúc'}
        </button>
      </div>
    </div>
  </div>
}
