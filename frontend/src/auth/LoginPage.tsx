import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { landingPath } from '../app/feature-routes'
import { ApiError } from '../shared/api'
import { ErrorMessage, Field } from '../shared/components'
import { login } from './login'
import { useSession } from './session'

export function LoginPage() {
  const { user } = useSession()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  if (user) {
    const destination = landingPath(user.role)
    if (destination !== '/login') return <Navigate to={destination} replace />
    return <section className="card"><p className="eyebrow">GDRN / TÀI KHOẢN</p><h1>Đã đăng nhập</h1>
      <p>{user.email}</p><p>Vai trò: {user.role === 'CITIZEN' ? 'Người dân' : 'Điều phối viên'}</p>
      <p className="muted">Phiên đăng nhập đã được xác nhận. Tải lại trang sẽ yêu cầu đăng nhập lại.</p></section>
  }
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy) return
    setError('')
    const fields: Record<string, string> = {}
    const normalized = email.trim().toLowerCase()
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalized) || [...normalized].length > 254) fields.email = 'Nhập địa chỉ email hợp lệ (tối đa 254 ký tự).'
    if (!password || [...password].length > 128 || new TextEncoder().encode(password).length > 72) fields.password = 'Nhập mật khẩu, tối đa 72 byte UTF-8.'
    setFieldErrors(fields)
    if (Object.keys(fields).length) return
    setBusy(true)
    try {
      await login(email, password)
      setPassword('')
    } catch (failure) {
      setPassword('')
      setError(failure instanceof ApiError && failure.problem?.code === 'INVALID_CREDENTIALS'
        ? 'Email hoặc mật khẩu không đúng.' : failure instanceof ApiError ? failure.message : 'Không thể đăng nhập. Vui lòng kiểm tra kết nối và thử lại.')
    } finally { setBusy(false) }
  }
  return <section className="card login-card"><p className="eyebrow">GLOBAL DISASTER RESPONSE NETWORK</p>
    <h1>Đăng nhập GDRN</h1><p className="muted">Kết nối người dân và đội ngũ điều phối.</p>
    <form onSubmit={submit} noValidate aria-busy={busy}>
      <Field id="email" label="Email" type="email" autoComplete="username" value={email} onChange={e => setEmail(e.target.value)} error={fieldErrors.email} required disabled={busy} />
      <Field id="password" label="Mật khẩu" type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} error={fieldErrors.password} required disabled={busy} />
      {error && <ErrorMessage>{error}</ErrorMessage>}
      <button type="submit" disabled={busy}>{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}</button>
    </form><p className="session-note">Phiên có thời hạn 15 phút. Tải lại trang cần đăng nhập lại.</p>
  </section>
}
