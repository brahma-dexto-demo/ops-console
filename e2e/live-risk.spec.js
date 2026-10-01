const { test, expect } = require('@playwright/test');

test('live risk scores, high-risk filtering, pagination, and detail', async ({ page }) => {
  test.skip(!!process.env.MOCK_API, 'Requires live Batch scores');
  await page.goto('/');
  await expect(page.locator('tbody .risk-badge').first()).toBeVisible();
  await page.getByLabel('High-risk only').check();
  await page.getByRole('button', { name: 'Apply filters' }).click();
  await expect(page).toHaveURL(/high_risk=true/);
  const count = await page.locator('tbody tr').count();
  expect(count).toBeGreaterThan(0);
  for (const badge of await page.locator('tbody .risk-badge').all()) {
    const text = await badge.innerText();
    expect(text).toMatch(/^High · (?:[7-9]\d|100)$/);
  }
  const firstName = await page.locator('tbody tr a').first().innerText();
  await page.getByRole('link', { name: firstName, exact: true }).click();
  await expect(page.getByRole('heading', { name: firstName, exact: true })).toBeVisible();
  await expect(page.locator('.risk-badge')).toHaveText(/^High · (?:[7-9]\d|100)$/);
  await page.goto('/?high_risk=true&offset=50');
  await expect(page.getByLabel('High-risk only')).toBeChecked();
  await expect(page.locator('tbody tr').first()).toBeVisible();
  for (const badge of await page.locator('tbody .risk-badge').all()) {
    expect(await badge.innerText()).toMatch(/^High · (?:[7-9]\d|100)$/);
  }
});
