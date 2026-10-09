import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../shared/api'
import type { Disaster, DisasterPage as Page } from './disaster-api'

const mocks = vi.hoisted(() => ({
  list: vi.fn(), get: vi.fn(), create: vi.fn(), update: vi.fn(),
}))
vi.mock('./disaster-api', async importOriginal => ({
  ...await importOriginal<typeof import('./disaster-api')>(),
  listDisasters: mocks.list,
  getDisaster: mocks.get,
  createDisaster: mocks.create,
  updateDisaster: mocks.update,
}))

import { DisasterPage } from './DisasterPage'

const active: Disaster = {
  id: '00000000-0000-0000-0000-000000000001',
  name: 'Lũ miền Trung', type: 'FLOOD', severity: 'HIGH', description: 'Nước đang dâng nhanh.',
  latitude: 16.0471, longitude: 108.2068, status: 'ACTIVE', version: 2,
  createdAt: '2026-10-09T10:00:00Z', updatedAt: '2026-10-09T10:10:00Z',
}
const page = (items: Disaster[] = [active]): Page => ({ items, page: 0, size: 20, totalElements: items.length, totalPages: items.length ? 1 : 0 })

describe('DisasterPage', () => {
  beforeEach(() => {
    mocks.list.mockResolvedValue(page())
    mocks.get.mockResolvedValue(active)
    mocks.create.mockResolvedValue(active)
    mocks.update.mockResolvedValue({ ...active, name: 'Lũ miền Trung mở rộng', version: 3 })
  })

  it('loads, filters, selects a detail and confirms resolve', async () => {
    render(<DisasterPage />)
    expect(await screen.findByText('Lũ miền Trung')).toBeVisible()
    await userEvent.selectOptions(screen.getByLabelText('Trạng thái'), 'ACTIVE')
    await waitFor(() => expect(mocks.list).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'ACTIVE', page: 0 })))

    await userEvent.click(screen.getByRole('button', { name: /Lũ miền Trung/ }))
    expect(await screen.findByRole('heading', { name: 'Lũ miền Trung' })).toBeVisible()
    await userEvent.click(screen.getByRole('button', { name: 'Kết thúc thảm họa' }))
    expect(screen.getByRole('alertdialog')).toHaveTextContent('không thể mở lại')
    await userEvent.click(screen.getByRole('button', { name: 'Xác nhận kết thúc' }))
    await waitFor(() => expect(mocks.update).toHaveBeenCalledWith(active.id, { expectedVersion: 2, status: 'RESOLVED' }))
  })

  it('shows empty and recoverable list errors', async () => {
    mocks.list.mockResolvedValueOnce(page([]))
    const view = render(<DisasterPage />)
    expect(await screen.findByText('Không có thảm họa phù hợp bộ lọc.')).toBeVisible()
    view.unmount()

    mocks.list.mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce(page())
    render(<DisasterPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Không thể kết nối')
    await userEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('Lũ miền Trung')).toBeVisible()
  })

  it('keeps a 409 visible and refreshes stale detail before another write', async () => {
    const refreshed = { ...active, name: 'Tên từ máy chủ', version: 3 }
    mocks.update.mockRejectedValueOnce(new ApiError(409))
    mocks.get.mockResolvedValueOnce(active).mockResolvedValueOnce(refreshed)
    render(<DisasterPage />)
    await userEvent.click(await screen.findByRole('button', { name: /Lũ miền Trung/ }))
    await screen.findByRole('heading', { name: 'Lũ miền Trung' })
    await userEvent.click(screen.getByRole('button', { name: 'Chỉnh sửa' }))
    const name = screen.getByLabelText('Tên thảm họa')
    await userEvent.clear(name)
    await userEvent.type(name, 'Tên do người dùng sửa')
    await userEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Dữ liệu đã thay đổi')
    expect(await screen.findByRole('heading', { name: 'Tên từ máy chủ' })).toBeVisible()
    expect(mocks.get).toHaveBeenCalledTimes(2)
  })
})
