import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { App, RoleGuard } from '../app/App'
import { session, type Role } from './session'

function mount() { render(<MemoryRouter initialEntries={['/login']}><App /></MemoryRouter>) }
async function fillAndSubmit() {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Email'), 'one@example.test')
  await user.type(screen.getByLabelText('Mật khẩu'), 'test-only-password')
  await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))
}
describe('Login', () => {
  it('validates before fetching', async () => {
    const fetcher = vi.fn(); vi.stubGlobal('fetch', fetcher)
    mount()
    await userEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Email')).toHaveFocus()
    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Nhập địa chỉ email hợp lệ (tối đa 254 ký tự).')
    expect(fetcher).not.toHaveBeenCalled()
  })
  it.each(['CITIZEN', 'AUTHORITY'] as Role[])('confirms %s through me and clears on logout', async role => {
    const user = { id: 'one', email: 'one@example.test', role }
    const fetcher = vi.fn().mockResolvedValueOnce(new Response(JSON.stringify({ accessToken: 'opaque', tokenType: 'Bearer', expiresIn: 900, expiresAt: new Date(Date.now() + 900_000).toISOString(), user })))
      .mockResolvedValueOnce(new Response(JSON.stringify(user)))
    vi.stubGlobal('fetch', fetcher)
    mount(); await fillAndSubmit()
    expect(await screen.findByRole('heading', { name: 'Đã đăng nhập' })).toBeVisible()
    expect(fetcher.mock.calls.map(call => call[0])).toEqual(['/api/auth/login', '/api/auth/me'])
    expect(session.snapshot().user).toEqual(user)
    expect(Boolean(screen.queryByRole('link', { name: 'Thảm họa' }))).toBe(role === 'AUTHORITY')
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
    await userEvent.click(screen.getByRole('button', { name: 'Đăng xuất' }))
    expect(screen.getByLabelText('Email')).toHaveValue('')
    expect(session.token()).toBeNull()
    expect(screen.queryByRole('navigation')).not.toBeInTheDocument()
  })
  it('announces a pending login, prevents duplicate submit and recovers after a network error', async () => {
    let reject!: (error: Error) => void
    const fetcher = vi.fn(() => new Promise<Response>((_, fail) => { reject = fail }))
    vi.stubGlobal('fetch', fetcher)
    mount(); await fillAndSubmit()
    expect(screen.getByRole('status')).toHaveTextContent('Đang xác thực phiên đăng nhập.')
    expect(screen.getByLabelText('Email')).toBeDisabled()
    expect(screen.getByLabelText('Mật khẩu')).toBeDisabled()
    const submit = screen.getByRole('button', { name: 'Đang đăng nhập…' })
    expect(submit).toBeDisabled()
    await userEvent.click(submit)
    expect(fetcher).toHaveBeenCalledOnce()
    reject(new Error('network unavailable'))
    expect(await screen.findByRole('alert')).toHaveTextContent('kiểm tra kết nối')
    expect(screen.getByRole('button', { name: 'Đăng nhập' })).toBeEnabled()
    expect(screen.getByLabelText('Email')).toHaveValue('one@example.test')
    expect(screen.getByLabelText('Mật khẩu')).toHaveValue('')
  })
  it('shows invalid credentials without losing the error during session clear', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 'INVALID_CREDENTIALS' }), { status: 401 })))
    mount(); await fillAndSubmit()
    expect(await screen.findByRole('alert')).toHaveTextContent('Email hoặc mật khẩu không đúng.')
    expect(screen.getByLabelText('Mật khẩu')).toHaveValue('')
  })
  it('does not establish a session when me fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(new Response(JSON.stringify({ accessToken: 'opaque', user: { id: 'one' } })))
      .mockResolvedValueOnce(new Response('{}', { status: 401 })))
    mount(); await fillAndSubmit()
    await waitFor(() => expect(screen.getByRole('alert')).toBeVisible())
    expect(session.token()).toBeNull()
  })
  it('denies wrong-role feature routes', () => {
    session.establish('opaque', { id: 'one', email: 'one@example.test', role: 'CITIZEN' }, new Date(Date.now() + 60_000).toISOString(), session.snapshot().generation)
    render(<MemoryRouter><RoleGuard roles={['AUTHORITY']}><p>private</p></RoleGuard></MemoryRouter>)
    expect(screen.getByRole('alert')).toBeVisible()
    expect(screen.queryByText('private')).not.toBeInTheDocument()
  })
})
