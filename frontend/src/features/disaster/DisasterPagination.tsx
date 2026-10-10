import type { DisasterPage } from './disaster-api'

export function DisasterPagination({ result, disabled, onPageChange }: {
  result: DisasterPage
  disabled: boolean
  onPageChange: (page: number) => void
}) {
  if (result.totalPages <= 1) return null
  const first = result.page * result.size + 1
  const last = Math.min((result.page + 1) * result.size, result.totalElements)
  return <div className="disaster-pagination">
    <p><span className="disaster-pagination__range">{first}–{last} / </span>{result.totalElements} thảm họa</p>
    <nav aria-label="Phân trang thảm họa">
      <button type="button" className="secondary" disabled={result.page === 0 || disabled}
        onClick={() => onPageChange(result.page - 1)}>Trang trước</button>
      <span aria-current="page">{result.page + 1} / {result.totalPages}</span>
      <button type="button" className="secondary" disabled={result.page + 1 >= result.totalPages || disabled}
        onClick={() => onPageChange(result.page + 1)}>Trang sau</button>
    </nav>
  </div>
}
