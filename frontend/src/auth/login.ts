import { api } from '../shared/api'
import { session, type User } from './session'

type LoginResponse = { accessToken: string; tokenType: 'Bearer'; expiresIn: number; expiresAt: string; user: User }
export async function login(email: string, password: string) {
  session.clear()
  const generation = session.snapshot().generation
  const result = await api<LoginResponse>('/api/auth/login', {
    method: 'POST', anonymous: true, body: JSON.stringify({ email: email.trim().toLowerCase(), password }),
  })
  if (generation !== session.snapshot().generation) throw new DOMException('Login cancelled', 'AbortError')
  const user = await api<User>('/api/auth/me', { bearer: result.accessToken })
  if (!['CITIZEN', 'AUTHORITY'].includes(user.role) || user.id !== result.user.id
    || !session.establish(result.accessToken, user, result.expiresAt, generation)) {
    throw new Error('Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.')
  }
}
