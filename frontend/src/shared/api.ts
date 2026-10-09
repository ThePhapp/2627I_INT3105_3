import { session } from '../auth/session'

export type ApiProblem = {
  timestamp: string; status: number; code: string; message: string; path: string
  fieldErrors: { field: string; message: string }[]
}
export class ApiError extends Error {
  constructor(public status: number, public problem?: ApiProblem) {
    super(status === 401 ? 'Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.'
      : status === 403 ? 'Bạn không có quyền thực hiện thao tác này.'
      : status === 409 ? 'Dữ liệu đã thay đổi. Hãy tải lại và kiểm tra trước khi tiếp tục.'
      : 'Không thể hoàn tất yêu cầu. Vui lòng thử lại.')
  }
}
type Options = Omit<RequestInit, 'credentials'> & { anonymous?: boolean; bearer?: string }
export async function api<T>(path: string, options: Options = {}): Promise<T> {
  if (!path.startsWith('/api/') || path.includes('://')) throw new Error('API path must be relative to /api/')
  const { anonymous, bearer, ...init } = options
  const currentToken = anonymous ? null : bearer ?? session.token()
  const generation = session.snapshot().generation
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body !== undefined) headers.set('Content-Type', 'application/json')
  if (currentToken) headers.set('Authorization', `Bearer ${currentToken}`)
  else headers.delete('Authorization')
  const response = await fetch(path, { ...init, headers, credentials: 'omit', cache: 'no-store' })
  if (!response.ok) {
    if (response.status === 401 && currentToken === session.token() && generation === session.snapshot().generation) session.clear()
    const problem = await response.json().catch(() => undefined) as ApiProblem | undefined
    throw new ApiError(response.status, problem)
  }
  // Do not deliver a previous user's late response into the next user's UI/cache.
  if (!anonymous && generation !== session.snapshot().generation) throw new DOMException('Session changed', 'AbortError')
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}
