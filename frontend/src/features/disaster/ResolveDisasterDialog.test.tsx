import { useRef, useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ResolveDisasterDialog } from './ResolveDisasterDialog'

function Harness({ onResolve = vi.fn() }: { onResolve?: () => void }) {
  const [open, setOpen] = useState(false)
  const trigger = useRef<HTMLButtonElement>(null)
  return <><button ref={trigger} type="button" onClick={() => setOpen(true)}>Mở xác nhận</button>
    {open && <ResolveDisasterDialog disasterName="Lũ miền Trung" busy={false}
      returnFocus={trigger.current} onResolve={onResolve} onCancel={() => setOpen(false)} />}</>
}

describe('ResolveDisasterDialog', () => {
  it('traps keyboard focus, closes with Escape and restores the trigger', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    const trigger = screen.getByRole('button', { name: 'Mở xác nhận' })
    await user.click(trigger)

    const cancel = screen.getByRole('button', { name: 'Quay lại' })
    const resolve = screen.getByRole('button', { name: 'Xác nhận kết thúc' })
    expect(cancel).toHaveFocus()
    await user.tab()
    expect(resolve).toHaveFocus()
    await user.tab()
    expect(cancel).toHaveFocus()

    await user.keyboard('{Escape}')
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
  })

  it('submits the irreversible action once selected', async () => {
    const resolve = vi.fn()
    const user = userEvent.setup()
    render(<Harness onResolve={resolve} />)
    await user.click(screen.getByRole('button', { name: 'Mở xác nhận' }))
    await user.click(screen.getByRole('button', { name: 'Xác nhận kết thúc' }))
    expect(resolve).toHaveBeenCalledTimes(1)
  })
})
