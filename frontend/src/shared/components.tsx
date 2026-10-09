import type { InputHTMLAttributes, ReactNode } from 'react'

export function Field({ label, error, id, ...input }: InputHTMLAttributes<HTMLInputElement> & { id: string; label: string; error?: string }) {
  return <div className="field"><label htmlFor={id}>{label}</label>
    <input id={id} {...input} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined} />
    {error && <span id={`${id}-error`} className="field-error">{error}</span>}
  </div>
}
export function ErrorMessage({ children }: { children: ReactNode }) {
  return <div className="notice error" role="alert">{children}</div>
}
export function Loading({ children = 'Đang tải…' }: { children?: ReactNode }) {
  return <p role="status" aria-live="polite">{children}</p>
}
export function EmptyState({ children }: { children: ReactNode }) {
  return <p className="notice">{children}</p>
}
