import type { InputHTMLAttributes, ReactNode } from 'react'

export function PageHeader({ eyebrow, title, description, action }: { eyebrow?: string; title: string; description?: ReactNode; action?: ReactNode }) {
  return <div className="page-header"><div>
    {eyebrow && <p className="eyebrow">{eyebrow}</p>}
    <h1>{title}</h1>{description && <p className="muted">{description}</p>}
  </div>{action && <div className="page-header__action">{action}</div>}</div>
}

export function Field({ label, error, hint, id, 'aria-describedby': describedBy, ...input }: InputHTMLAttributes<HTMLInputElement> & { id: string; label: string; error?: string; hint?: string }) {
  const description = [describedBy, hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(' ') || undefined
  return <div className="field"><label htmlFor={id}>{label}</label>
    <input id={id} {...input} aria-invalid={Boolean(error)} aria-describedby={description} />
    {hint && <span id={`${id}-hint`} className="field-hint">{hint}</span>}
    {error && <span id={`${id}-error`} className="field-error">{error}</span>}
  </div>
}
export function ErrorMessage({ children }: { children: ReactNode }) {
  return <div className="notice error" role="alert">{children}</div>
}
export function Loading({ children = 'Đang tải…' }: { children?: ReactNode }) {
  return <p className="loading-state" role="status" aria-live="polite"><span className="loading-indicator" aria-hidden="true" />{children}</p>
}
export function EmptyState({ children }: { children: ReactNode }) {
  return <p className="notice empty-state">{children}</p>
}
