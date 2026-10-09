import { describe, it, expect, vi } from 'vitest'
import { api, ApiError } from './api'
import { session } from '../auth/session'

const user = { id: 'one', email: 'one@example.test', role: 'CITIZEN' as const }
function establish(token = 'opaque') {
  session.establish(token, user, new Date(Date.now() + 60_000).toISOString(), session.snapshot().generation)
}
describe('API client and memory session', () => {
  it('sends Bearer without cookies and clears user state on 401', async () => {
    establish()
    const cleared = vi.fn()
    const unsubscribe = session.onClear(cleared)
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 'UNAUTHENTICATED' }), { status: 401 }))
    vi.stubGlobal('fetch', fetcher)
    await expect(api('/api/auth/me')).rejects.toBeInstanceOf(ApiError)
    const init = fetcher.mock.calls[0][1]
    expect(init.headers.get('Authorization')).toBe('Bearer opaque')
    expect(init.credentials).toBe('omit')
    expect(session.snapshot().user).toBeNull()
    expect(session.token()).toBeNull()
    expect(cleared).toHaveBeenCalledOnce()
    unsubscribe()
  })
  it('keeps session on 403 and never automatically retries a 409 write', async () => {
    establish()
    const fetcher = vi.fn().mockResolvedValueOnce(new Response('{}', { status: 403 }))
      .mockResolvedValueOnce(new Response('{}', { status: 409 }))
    vi.stubGlobal('fetch', fetcher)
    await expect(api('/api/future')).rejects.toMatchObject({ status: 403 })
    await expect(api('/api/future', { method: 'POST', body: '{}' })).rejects.toMatchObject({ status: 409 })
    expect(fetcher).toHaveBeenCalledTimes(2)
    expect(session.snapshot().user).toEqual(user)
  })
  it('does not let a previous user late 401 erase a new session', async () => {
    establish('old')
    let respond!: (value: Response) => void
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(resolve => { respond = resolve })))
    const request = api('/api/auth/me')
    session.clear(); establish('new')
    respond(new Response('{}', { status: 401 }))
    await expect(request).rejects.toMatchObject({ status: 401 })
    expect(session.token()).toBe('new')
  })
  it('expires in memory and rejects old successful responses after logout', async () => {
    vi.useFakeTimers()
    establish()
    vi.advanceTimersByTime(60_001)
    expect(session.token()).toBeNull()
    vi.useRealTimers()
    establish()
    let respond!: (value: Response) => void
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(resolve => { respond = resolve })))
    const request = api('/api/auth/me')
    session.clear()
    respond(new Response('{}'))
    await expect(request).rejects.toMatchObject({ name: 'AbortError' })
  })
})
