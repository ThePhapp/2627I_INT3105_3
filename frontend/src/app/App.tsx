import type { ReactNode } from 'react'
import { Link, NavLink, Navigate, Route, Routes } from 'react-router-dom'
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
    <header className={`app-header${user?.role === 'AUTHORITY' ? ' app-header--authority' : ''}`}><Link className="brand" to={user ? landingPath(user.role) : '/login'}><span className="brand-mark" aria-hidden="true">G</span><span className="brand-name">GDRN<span className="brand-caption">Crisis Command</span></span></Link>
      {user ? <nav className="app-nav" aria-label="Điều hướng chính"><span className="app-role">{user.role === 'CITIZEN' ? 'Người dân' : 'Điều phối viên'}</span>{featureRoutes.filter(route => route.roles.includes(user.role)).map(route => <NavLink key={route.path} to={route.path}>{route.label}</NavLink>)}
        <button className="secondary" onClick={() => session.clear()}>Đăng xuất</button></nav>
        : <span className="app-header__note">Kết nối cộng đồng. Phối hợp ứng phó.</span>}
    </header><main id="main" className="app-main" tabIndex={-1}><Routes>
      <Route path="/login" element={<LoginPage key={user?.id ?? 'anonymous'} />} />
      <Route path="/" element={<Navigate to={user ? landingPath(user.role) : '/login'} replace />} />
      {featureRoutes.map(route => <Route key={route.path} path={route.path} element={<RoleGuard key={generation} roles={route.roles}>{route.element}</RoleGuard>} />)}
      <Route path="*" element={user ? <EmptyState>Không tìm thấy trang này.</EmptyState> : <Navigate to="/login" replace />} />
    </Routes></main><footer className="app-footer">GDRN · Mô phỏng phục vụ học tập</footer></>
}
