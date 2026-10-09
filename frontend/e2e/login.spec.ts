import { test, expect } from '@playwright/test'

for (const prefix of ['DEMO_CITIZEN_ONE', 'DEMO_CITIZEN_TWO', 'DEMO_AUTHORITY']) {
  test(`${prefix}: real login, reload, logout and responsive form`, async ({ page }) => {
    const email = process.env[`${prefix}_EMAIL`]
    const password = process.env[`${prefix}_PASSWORD`]
    if (!email || !password) throw new Error(`Set ${prefix}_EMAIL and ${prefix}_PASSWORD for the real demo backend`)
    await page.setViewportSize({ width: 390, height: 844 })
    await page.goto('/login')
    if (prefix === 'DEMO_CITIZEN_ONE') await page.screenshot({ path: 'test-results/p01-login.png', fullPage: true })
    await page.getByLabel('Email', { exact: true }).fill(email)
    await page.getByLabel('Mật khẩu', { exact: true }).fill('incorrect-test-password')
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    await expect(page.getByRole('alert')).toContainText('Email hoặc mật khẩu không đúng.')
    await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Đã đăng nhập' })).toBeVisible()
    await expect(page.getByText(email, { exact: true })).toBeVisible()
    expect(await page.evaluate(() => localStorage.length + sessionStorage.length)).toBe(0)
    await page.reload()
    await expect(page.getByRole('heading', { name: 'Đăng nhập GDRN' })).toBeVisible()
    await page.getByLabel('Email', { exact: true }).fill(email)
    await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Đã đăng nhập' })).toBeVisible()
    await page.getByRole('button', { name: 'Đăng xuất' }).click()
    await expect(page.getByLabel('Email', { exact: true })).toHaveValue('')
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  })
}
