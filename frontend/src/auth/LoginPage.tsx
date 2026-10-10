import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { landingPath } from '../app/feature-routes'
import { ApiError } from '../shared/api'
import { ErrorMessage, Field, PageHeader } from '../shared/components'
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
    return <section className="card account-card"><PageHeader eyebrow="GDRN / TÀI KHOẢN" title="Đã đăng nhập" />
      <dl className="account-facts"><div><dt>Email</dt><dd>{user.email}</dd></div><div><dt>Vai trò</dt><dd>{user.role === 'CITIZEN' ? 'Người dân' : 'Điều phối viên'}</dd></div></dl>
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
    if (Object.keys(fields).length) {
      document.getElementById(fields.email ? 'email' : 'password')?.focus()
      return
    }
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
  return <div className="login-screen">
    <section className="login-hero" aria-labelledby="login-hero-title">
      <div className="login-topography" aria-hidden="true" />
      <div className="login-hero__content"><p className="eyebrow">Global Disaster Response Network</p>
        <h2 id="login-hero-title">Kết nối để<br />ứng phó kịp thời<span className="hero-period">.</span></h2>
        <p className="login-hero__description">Một điểm kết nối giữa người dân và đội ngũ điều phối trong mô phỏng ứng phó thảm họa.</p>
      </div>
      <div className="login-hero__signature"><span aria-hidden="true" className="hero-rule" /><span>GDRN / CRISIS COMMAND</span><span>Rõ thông tin. Vững phối hợp.</span></div>
    </section>
    <section className="login-card" aria-label="Đăng nhập">
    <PageHeader eyebrow="CHÀO MỪNG TRỞ LẠI" title="Đăng nhập GDRN" description="Sử dụng tài khoản được cấp để tiếp tục." />
    <form onSubmit={submit} noValidate aria-busy={busy}>
      <Field id="email" label="Email" type="email" autoComplete="username" value={email} onChange={e => setEmail(e.target.value)} error={fieldErrors.email} required disabled={busy} />
      <Field id="password" label="Mật khẩu" type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} error={fieldErrors.password} required disabled={busy} />
      {error && <ErrorMessage>{error}</ErrorMessage>}
      <button className="login-submit" type="submit" disabled={busy}>{busy ? 'Đang đăng nhập…' : 'Đăng nhập'}<span aria-hidden="true">{busy ? '…' : '→'}</span></button>
      <span className="sr-only" role="status">{busy ? 'Đang xác thực phiên đăng nhập.' : ''}</span>
    </form><p className="session-note">Phiên có thời hạn 15 phút.<br />Tải lại trang cần đăng nhập lại.</p>
    </section>
  </div>
}
