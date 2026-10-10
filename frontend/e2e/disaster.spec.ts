import { expect, test } from '@playwright/test'

test('authority creates, edits and resolves a disaster across UI01-C1 viewports', async ({ page }) => {
  const email = process.env.DEMO_AUTHORITY_EMAIL
  const password = process.env.DEMO_AUTHORITY_PASSWORD
  if (!email || !password) throw new Error('Set DEMO_AUTHORITY_EMAIL and DEMO_AUTHORITY_PASSWORD for the real demo backend')

  await page.setViewportSize({ width: 1280, height: 900 })
  await page.goto('/login')
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
  await page.getByRole('link', { name: 'Thảm họa', exact: true }).click()
  await expect(page).toHaveURL(/\/operations\/disasters$/)
  await expect(page.getByRole('heading', { name: 'Quản lý thảm họa' })).toBeVisible()

  const name = `Kiểm thử C1 ${Date.now()}`
  await page.getByRole('button', { name: 'Tạo thảm họa', exact: true }).click()
  await page.getByLabel('Tên thảm họa').fill(name)
  await page.getByLabel('Loại thảm họa').selectOption('FLOOD')
  await page.getByLabel('Mức độ').selectOption('HIGH')
  await page.getByLabel('Mô tả').fill('Thảm họa được tạo qua luồng UI thật của C1.')
  await page.getByLabel('Vĩ độ').fill('16.0471')
  await page.getByLabel('Kinh độ').fill('108.2068')
  await page.getByRole('complementary', { name: 'Tạo thảm họa' })
    .getByRole('button', { name: 'Tạo thảm họa', exact: true }).click()
  await expect(page.getByRole('heading', { name })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: 'test-results/ui01-c1-disaster-1280.png', fullPage: true })

  await page.getByRole('button', { name: 'Chỉnh sửa', exact: true }).click()
  await page.getByLabel('Mức độ').selectOption('CRITICAL')
  await page.getByRole('button', { name: 'Lưu thay đổi', exact: true }).click()
  await expect(page.getByRole('complementary', { name: 'Chi tiết thảm họa' })
    .getByText('Nghiêm trọng', { exact: true })).toBeVisible()

  await page.setViewportSize({ width: 768, height: 900 })
  await expect(page.getByRole('heading', { name })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: 'test-results/ui01-c1-disaster-768.png', fullPage: true })

  await page.getByRole('button', { name: 'Kết thúc thảm họa', exact: true }).click()
  await expect(page.getByRole('alertdialog')).toContainText('không thể mở lại')
  await expect(page.getByRole('button', { name: 'Quay lại' })).toBeFocused()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('alertdialog')).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Kết thúc thảm họa', exact: true })).toBeFocused()

  await page.setViewportSize({ width: 360, height: 800 })
  await page.getByRole('button', { name: 'Kết thúc thảm họa', exact: true }).click()
  await expect(page.getByRole('alertdialog')).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: 'test-results/ui01-c1-disaster-360-confirm.png', fullPage: true })
  await page.getByRole('button', { name: 'Xác nhận kết thúc', exact: true }).click()
  await expect(page.getByRole('complementary', { name: 'Chi tiết thảm họa' })
    .getByText('Đã kết thúc', { exact: true })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Quản lý thảm họa' })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.screenshot({ path: 'test-results/ui01-c1-disaster-360-resolved.png', fullPage: true })
})
