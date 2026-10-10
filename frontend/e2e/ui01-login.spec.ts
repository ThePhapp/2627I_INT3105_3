import { expect, test } from '@playwright/test'

for (const width of [360, 768, 1280]) {
  test(`S01 layout and keyboard at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 })
    await page.emulateMedia({ reducedMotion: 'reduce' })
    await page.goto('/login')
    await expect(page.getByRole('heading', { name: 'Đăng nhập GDRN' })).toBeVisible()
    await page.keyboard.press('Tab')
    await expect(page.getByRole('link', { name: 'Đến nội dung chính' })).toBeFocused()
    await page.keyboard.press('Enter')
    await expect(page.getByRole('main')).toBeFocused()
    await page.keyboard.press('Tab')
    await expect(page.getByLabel('Email', { exact: true })).toBeFocused()
    expect(await page.getByLabel('Email', { exact: true }).evaluate(el => getComputedStyle(el).outlineStyle)).toBe('solid')
    await page.keyboard.press('Tab')
    await expect(page.getByLabel('Mật khẩu', { exact: true })).toBeFocused()
    await page.keyboard.press('Tab')
    await page.keyboard.press('Enter')
    await expect(page.getByLabel('Email', { exact: true })).toBeFocused()
    await expect(page.getByLabel('Email', { exact: true })).toHaveAttribute('aria-invalid', 'true')
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.screenshot({ path: `test-results/ui01-login-${width}-validation.png`, fullPage: true })
    await page.reload()
    await page.screenshot({ path: `test-results/ui01-login-${width}.png`, fullPage: true })
    // Text enlargement checks reflow; native browser zoom is a separate manual check.
    await page.evaluate(() => { document.documentElement.style.fontSize = '200%' })
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await expect(page.getByRole('button', { name: 'Đăng nhập', exact: true })).toBeVisible()
  })
}

test('real role navigation and forbidden route preserve citizen session; logout clears it', async ({ page }) => {
  const email = process.env.DEMO_CITIZEN_ONE_EMAIL
  const password = process.env.DEMO_CITIZEN_ONE_PASSWORD
  if (!email || !password) throw new Error('Configure DEMO_CITIZEN_ONE credentials in environment')
  await page.goto('/operations/disasters')
  await expect(page).toHaveURL(/\/login$/)
  await page.getByLabel('Email', { exact: true }).fill(email)
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Đã đăng nhập' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Thảm họa', exact: true })).toHaveCount(0)
  // SPA navigation keeps the in-memory session, so this exercises RoleGuard rather than reload.
  await page.evaluate(() => { history.pushState(null, '', '/operations/disasters'); dispatchEvent(new PopStateEvent('popstate')) })
  await expect(page.getByRole('alert')).toHaveText('Bạn không có quyền truy cập trang này.')
  await expect(page.getByRole('button', { name: 'Đăng xuất' })).toBeVisible()
  await page.getByRole('button', { name: 'Đăng xuất' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByLabel('Email', { exact: true })).toHaveValue('')
  await expect(page.getByRole('navigation')).toHaveCount(0)
  expect(await page.evaluate(() => localStorage.length + sessionStorage.length)).toBe(0)
})
