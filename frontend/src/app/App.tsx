import type { ReactNode } from 'react'
import { Link, Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '../auth/LoginPage'
import { session, useSession, type Role } from '../auth/session'
import { EmptyState, ErrorMessage } from '../shared/components'
import { featureRoutes, landingPath } from './feature-routes'

export function RoleGuard({ roles, children }: { roles: readonly Role[]; children: ReactNode }) {
  const { user } = useSession()
  if (!user) return <Navigate to="/login" replace />
  return roles.includes(user.role) ? children : <ErrorMessage>Bạn không có quyền truy cập trang này.</ErrorMessage>
}
export function App() {
  const { user, generation } = useSession()
  return <><a className="skip-link" href="#main">Đến nội dung chính</a>
    <header><Link className="brand" to={user ? landingPath(user.role) : '/login'}>GDRN<span>Ứng phó thảm họa</span></Link>
      {user && <nav aria-label="Điều hướng chính">{featureRoutes.filter(route => route.roles.includes(user.role)).map(route => <Link key={route.path} to={route.path}>{route.label}</Link>)}
        <button className="secondary" onClick={() => session.clear()}>Đăng xuất</button></nav>}
    </header><main id="main"><Routes>
      <Route path="/login" element={<LoginPage key={user?.id ?? 'anonymous'} />} />
      <Route path="/" element={<Navigate to={user ? landingPath(user.role) : '/login'} replace />} />
      {featureRoutes.map(route => <Route key={route.path} path={route.path} element={<RoleGuard key={generation} roles={route.roles}>{route.element}</RoleGuard>} />)}
      <Route path="*" element={user ? <EmptyState>Không tìm thấy trang này.</EmptyState> : <Navigate to="/login" replace />} />
    </Routes></main><footer>GDRN · Mô phỏng phục vụ học tập</footer></>
}
